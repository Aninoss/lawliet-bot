package mysql.modules.guild;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.Language;
import core.LocalFile;
import core.ShardManager;
import modules.fishery.FisheryStatus;
import mysql.DBDataLoadAll;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

public class DBGuild extends DBObserverMapCache<Long, GuildData> {

    private static final DBGuild ourInstance = new DBGuild();

    public static DBGuild getInstance() {
        return ourInstance;
    }

    private DBGuild() {
    }

    private final Cache<Long, Boolean> removedServerIds = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    @Override
    protected GuildData load(Long serverId) throws Exception {
        boolean serverPresent = ShardManager.getLocalGuildById(serverId).isPresent();
        if (serverPresent) {
            removedServerIds.invalidate(serverId);
        }

        return MySQLManager.get(
                "SELECT prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantPowerups, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, powerPlantVCHoursCap, commandAuthorMessageRemove, fisheryCoinsGivenLimit, big FROM DServer WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        Integer fisheryVcHoursCap = resultSet.getInt(11);
                        if (resultSet.wasNull()) {
                            fisheryVcHoursCap = null;
                        }
                        return new GuildData(
                                serverId,
                                resultSet.getString(1),
                                new Locale(resultSet.getString(2).toLowerCase()),
                                FisheryStatus.valueOf(resultSet.getString(3)),
                                resultSet.getBoolean(4),
                                resultSet.getLong(5),
                                resultSet.getBoolean(6),
                                resultSet.getBoolean(7),
                                resultSet.getBoolean(8),
                                resultSet.getLong(9),
                                resultSet.getLong(10),
                                fisheryVcHoursCap,
                                resultSet.getBoolean(12),
                                resultSet.getBoolean(13),
                                resultSet.getBoolean(14)
                        );
                    } else {
                        GuildData guildBean = new GuildData(
                                serverId,
                                "L.",
                                Language.EN.getLocale(),
                                FisheryStatus.STOPPED,
                                false,
                                null,
                                true,
                                true,
                                true,
                                50000,
                                800000000,
                                null,
                                false,
                                true,
                                false
                        );
                        insertData(guildBean);
                        return guildBean;
                    }
                }
        );
    }

    public List<GuildKickedData> retrieveKickedData(long guildIdOffset, int limit) {
        return new DBDataLoadAll<GuildKickedData>("DServer", "serverId, kicked", " AND serverId > " + guildIdOffset + " ORDER BY serverId LIMIT " + limit)
                .getList(
                        resultSet -> {
                            Date date = resultSet.getDate(2);
                            return new GuildKickedData(
                                    resultSet.getLong(1),
                                    date != null ? date.toLocalDate() : null
                            );
                        }
                );
    }

    public boolean containsServerId(long serverId) {
        return !removedServerIds.asMap().containsKey(serverId);
    }

    private void insertData(GuildData guildData) {
        try {
            MySQLManager.update("INSERT INTO DServer (serverId, prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantPowerups, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, powerPlantVCHoursCap, commandAuthorMessageRemove, fisheryCoinsGivenLimit, big) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
                preparedStatement.setLong(1, guildData.getGuildId());
                preparedStatement.setString(2, guildData.getPrefix());
                preparedStatement.setString(3, guildData.getLocale().getDisplayName());
                preparedStatement.setString(4, guildData.getFisheryStatus().name());
                preparedStatement.setBoolean(5, guildData.isFisherySingleRoles());

                Optional<Long> announcementChannelIdOpt = guildData.getFisheryAnnouncementChannelId();
                if (announcementChannelIdOpt.isPresent()) {
                    preparedStatement.setLong(6, announcementChannelIdOpt.get());
                } else {
                    preparedStatement.setNull(6, Types.BIGINT);
                }

                preparedStatement.setBoolean(7, guildData.isFisheryTreasureChests());
                preparedStatement.setBoolean(8, guildData.isFisheryPowerups());
                preparedStatement.setBoolean(9, guildData.isFisheryReminders());
                preparedStatement.setLong(10, guildData.getFisheryRoleMin());
                preparedStatement.setLong(11, guildData.getFisheryRoleMax());

                Optional<Integer> VCHoursOpt = guildData.getFisheryVcHoursCap();
                if (VCHoursOpt.isPresent()) {
                    preparedStatement.setInt(12, VCHoursOpt.get());
                } else {
                    preparedStatement.setNull(12, Types.INTEGER);
                }

                preparedStatement.setBoolean(13, guildData.isCommandAuthorMessageRemove());
                preparedStatement.setBoolean(14, guildData.hasFisheryCoinsGivenLimit());
                preparedStatement.setBoolean(15, guildData.isBig());
            });
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void save(GuildData guildData) {
        MySQLManager.asyncUpdate("UPDATE DServer SET prefix = ?, locale = ?, powerPlant = ?, powerPlantSingleRole = ?, powerPlantAnnouncementChannelId = ?, powerPlantTreasureChests = ?, powerPlantPowerups = ?, powerPlantReminders = ?, powerPlantRoleMin = ?, powerPlantRoleMax = ?, powerPlantVCHoursCap = ?, commandAuthorMessageRemove = ?, fisheryCoinsGivenLimit = ?, big = ? WHERE serverId = ?;", preparedStatement -> {
            preparedStatement.setLong(12, guildData.getGuildId());

            preparedStatement.setString(1, guildData.getPrefix());
            preparedStatement.setString(2, guildData.getLocale().getDisplayName());
            preparedStatement.setString(3, guildData.getFisheryStatus().name());
            preparedStatement.setBoolean(4, guildData.isFisherySingleRoles());

            Optional<Long> announcementChannelIdOpt = guildData.getFisheryAnnouncementChannelId();
            if (announcementChannelIdOpt.isPresent()) {
                preparedStatement.setLong(5, announcementChannelIdOpt.get());
            } else {
                preparedStatement.setNull(5, Types.BIGINT);
            }

            preparedStatement.setBoolean(6, guildData.isFisheryTreasureChests());
            preparedStatement.setBoolean(7, guildData.isFisheryPowerups());
            preparedStatement.setBoolean(8, guildData.isFisheryReminders());
            preparedStatement.setLong(9, guildData.getFisheryRoleMin());
            preparedStatement.setLong(10, guildData.getFisheryRoleMax());

            Optional<Integer> VCHoursOpt = guildData.getFisheryVcHoursCap();
            if (VCHoursOpt.isPresent()) {
                preparedStatement.setInt(11, VCHoursOpt.get());
            } else {
                preparedStatement.setNull(11, Types.INTEGER);
            }

            preparedStatement.setBoolean(12, guildData.isCommandAuthorMessageRemove());
            preparedStatement.setBoolean(13, guildData.hasFisheryCoinsGivenLimit());
            preparedStatement.setBoolean(14, guildData.isBig());

            preparedStatement.setLong(15, guildData.getGuildId());
        });
    }

    public void setKicked(long guildId, LocalDate kicked) {
        MySQLManager.asyncUpdate("UPDATE DServer SET kicked = ? WHERE serverId = ?;", preparedStatement -> {
            if (kicked != null) {
                preparedStatement.setDate(1, Date.valueOf(kicked));
            } else {
                preparedStatement.setNull(1, Types.DATE);
            }
            preparedStatement.setLong(2, guildId);
        });
    }

    public void remove(long guildId) {
        removedServerIds.put(guildId, true);
        MySQLManager.asyncUpdate("DELETE FROM DServer WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, guildId));
        MySQLManager.invalidateGuildId(guildId);

        LocalFile welcomeBackgroundFile = new LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", guildId));
        if (welcomeBackgroundFile.exists()) {
            welcomeBackgroundFile.delete();
        }
    }

}
