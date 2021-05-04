package mysql.modules.guild;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.FisheryStatus;
import constants.Language;
import core.LocalFile;
import core.ShardManager;
import mysql.DBMain;
import mysql.DBObserverMapCache;

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
        int shard = ShardManager.getInstance().getResponsibleShard(serverId);
        if (shard < ShardManager.getInstance().getShardIntervalMin() || shard > ShardManager.getInstance().getShardIntervalMax()) {
            throw new Exception("Invalid server");
        }

        boolean serverPresent = ShardManager.getInstance().getLocalGuildById(serverId).isPresent();
        if (serverPresent) {
            removedServerIds.invalidate(serverId);
        }

        GuildData guildBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, powerPlantVCHoursCap, commandAuthorMessageRemove, fisheryCoinsGivenLimit FROM DServer WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            guildBean = new GuildData(
                    serverId,
                    resultSet.getString(1),
                    new Locale(resultSet.getString(2)),
                    FisheryStatus.valueOf(resultSet.getString(3)),
                    resultSet.getBoolean(4),
                    resultSet.getLong(5),
                    resultSet.getBoolean(6),
                    resultSet.getBoolean(7),
                    resultSet.getLong(8),
                    resultSet.getLong(9),
                    resultSet.getInt(10),
                    resultSet.getBoolean(11),
                    resultSet.getBoolean(12)

            );
        } else {
            guildBean = new GuildData(
                    serverId,
                    "L.",
                    Language.EN.getLocale(),
                    FisheryStatus.STOPPED,
                    false,
                    null,
                    true,
                    true,
                    50000,
                    800000000,
                    0,
                    false,
                    true
            );
            insertBean(guildBean);
        }

        resultSet.close();
        preparedStatement.close();

        return guildBean;
    }

    public boolean containsServerId(long serverId) {
        return !removedServerIds.asMap().containsKey(serverId);
    }

    private void insertBean(GuildData guildBean) throws SQLException, InterruptedException {
        DBMain.getInstance().update("INSERT INTO DServer (serverId, prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, powerPlantVCHoursCap, commandAuthorMessageRemove, fisheryCoinsGivenLimit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, guildBean.getGuildId());
            preparedStatement.setString(2, guildBean.getPrefix());
            preparedStatement.setString(3, guildBean.getLocale().getDisplayName());
            preparedStatement.setString(4, guildBean.getFisheryStatus().name());
            preparedStatement.setBoolean(5, guildBean.isFisherySingleRoles());

            Optional<Long> announcementChannelIdOpt = guildBean.getFisheryAnnouncementChannelId();
            if (announcementChannelIdOpt.isPresent()) {
                preparedStatement.setLong(6, announcementChannelIdOpt.get());
            } else {
                preparedStatement.setNull(6, Types.BIGINT);
            }

            preparedStatement.setBoolean(7, guildBean.isFisheryTreasureChests());
            preparedStatement.setBoolean(8, guildBean.isFisheryReminders());
            preparedStatement.setLong(9, guildBean.getFisheryRoleMin());
            preparedStatement.setLong(10, guildBean.getFisheryRoleMax());

            Optional<Integer> VCHoursOpt = guildBean.getFisheryVcHoursCap();
            if (VCHoursOpt.isPresent()) {
                preparedStatement.setInt(11, VCHoursOpt.get());
            } else {
                preparedStatement.setNull(11, Types.INTEGER);
            }

            preparedStatement.setBoolean(12, guildBean.isCommandAuthorMessageRemove());
            preparedStatement.setBoolean(13, guildBean.hasFisheryCoinsGivenLimit());
        });
    }

    @Override
    protected void save(GuildData guildBean) {
        DBMain.getInstance().asyncUpdate("UPDATE DServer SET prefix = ?, locale = ?, powerPlant = ?, powerPlantSingleRole = ?, powerPlantAnnouncementChannelId = ?, powerPlantTreasureChests = ?, powerPlantReminders = ?, powerPlantRoleMin = ?, powerPlantRoleMax = ?, powerPlantVCHoursCap = ?, commandAuthorMessageRemove = ?, fisheryCoinsGivenLimit = ? WHERE serverId = ?;", preparedStatement -> {
            preparedStatement.setLong(11, guildBean.getGuildId());

            preparedStatement.setString(1, guildBean.getPrefix());
            preparedStatement.setString(2, guildBean.getLocale().getDisplayName());
            preparedStatement.setString(3, guildBean.getFisheryStatus().name());
            preparedStatement.setBoolean(4, guildBean.isFisherySingleRoles());

            Optional<Long> announcementChannelIdOpt = guildBean.getFisheryAnnouncementChannelId();
            if (announcementChannelIdOpt.isPresent()) {
                preparedStatement.setLong(5, announcementChannelIdOpt.get());
            } else {
                preparedStatement.setNull(5, Types.BIGINT);
            }

            preparedStatement.setBoolean(6, guildBean.isFisheryTreasureChests());
            preparedStatement.setBoolean(7, guildBean.isFisheryReminders());
            preparedStatement.setLong(8, guildBean.getFisheryRoleMin());
            preparedStatement.setLong(9, guildBean.getFisheryRoleMax());

            Optional<Integer> VCHoursOpt = guildBean.getFisheryVcHoursCap();
            if (VCHoursOpt.isPresent()) {
                preparedStatement.setInt(10, VCHoursOpt.get());
            } else {
                preparedStatement.setNull(10, Types.INTEGER);
            }

            preparedStatement.setBoolean(11, guildBean.isCommandAuthorMessageRemove());
            preparedStatement.setBoolean(12, guildBean.hasFisheryCoinsGivenLimit());
            preparedStatement.setLong(13, guildBean.getGuildId());
        });
    }

    public void remove(long guildId) {
        removedServerIds.put(guildId, true);
        DBMain.getInstance().asyncUpdate("DELETE FROM DServer WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, guildId));
        DBMain.getInstance().invalidateGuildId(guildId);

        LocalFile welcomeBackgroundFile = new LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", guildId));
        if (welcomeBackgroundFile.exists()) {
            welcomeBackgroundFile.delete();
        }
    }

}
