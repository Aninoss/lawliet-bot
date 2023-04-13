package mysql.modules.reactionroles;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import core.CustomObservableMap;
import core.MainLogger;
import mysql.DBBatch;
import mysql.DBDataLoad;
import mysql.DBMapCache;
import mysql.MySQLManager;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class DBReactionRoles extends DBMapCache<Long, CustomObservableMap<Long, ReactionRoleMessage>> {

    private static final DBReactionRoles ourInstance = new DBReactionRoles();

    public static DBReactionRoles getInstance() {
        return ourInstance;
    }

    private DBReactionRoles() {
    }

    @Override
    protected CustomObservableMap<Long, ReactionRoleMessage> load(Long serverId) throws Exception {
        Map<Long, ReactionRoleMessage> reactionRolesMessageMap = new DBDataLoad<ReactionRoleMessage>("ReactionRolesMessage", "channelId, messageId, title, `desc`, image, roleRemoval, multipleRoles, newComponents, showRoleNumbers, showRoleConnections", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getMap(
                ReactionRoleMessage::getMessageId,
                resultSet -> {
                    long messageId = resultSet.getLong(2);
                    Boolean showRoleConnections = resultSet.getBoolean(10);
                    if (resultSet.wasNull()) {
                        showRoleConnections = null;
                    }
                    return new ReactionRoleMessage(
                            serverId,
                            resultSet.getLong(1),
                            messageId,
                            resultSet.getString(3),
                            resultSet.getString(4),
                            resultSet.getString(5),
                            resultSet.getBoolean(6),
                            resultSet.getBoolean(7),
                            ReactionRoleMessage.ComponentType.values()[resultSet.getInt(8)],
                            resultSet.getBoolean(9),
                            showRoleConnections,
                            getReactionRoleMessageSlots(serverId, messageId)
                    );
                }
        );

        return new CustomObservableMap<>(reactionRolesMessageMap)
                .addMapAddListener(this::addReactionRoleMessage)
                .addMapUpdateListener(this::addReactionRoleMessage)
                .addMapRemoveListener(this::removeReactionRoleMessage);
    }

    private List<ReactionRoleMessageSlot> getReactionRoleMessageSlots(long serverId, long messageId) {
        return new DBDataLoad<ReactionRoleMessageSlot>("ReactionRolesMessageSlot", "emoji, roleId", "messageId = ? ORDER BY slotId",
                preparedStatement -> preparedStatement.setLong(1, messageId)
        ).getList(
                resultSet -> {
                    String emojiString = resultSet.getString(1);
                    return new ReactionRoleMessageSlot(
                            serverId,
                            emojiString != null ? Emoji.fromFormatted(emojiString) : null,
                            resultSet.getLong(2)
                    );
                }
        );
    }

    private void addReactionRoleMessage(ReactionRoleMessage reactionRoleMessage) {
        MySQLManager.asyncUpdate("REPLACE INTO ReactionRolesMessage (serverId, channelId, messageId, title, `desc`, image, roleRemoval, multipleRoles, newComponents, showRoleNumbers, showRoleConnections) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, reactionRoleMessage.getGuildId());
            preparedStatement.setLong(2, reactionRoleMessage.getStandardGuildMessageChannelId());
            preparedStatement.setLong(3, reactionRoleMessage.getMessageId());

            if (reactionRoleMessage.getTitle() != null) {
                preparedStatement.setString(4, reactionRoleMessage.getTitle());
            } else {
                preparedStatement.setNull(4, Types.VARCHAR);
            }

            if (reactionRoleMessage.getDesc() != null) {
                preparedStatement.setString(5, reactionRoleMessage.getDesc());
            } else {
                preparedStatement.setNull(5, Types.VARCHAR);
            }

            if (reactionRoleMessage.getImage() != null) {
                preparedStatement.setString(6, reactionRoleMessage.getImage());
            } else {
                preparedStatement.setNull(6, Types.VARCHAR);
            }

            preparedStatement.setBoolean(7, reactionRoleMessage.getRoleRemoval());
            preparedStatement.setBoolean(8, reactionRoleMessage.getMultipleRoles());
            preparedStatement.setInt(9, reactionRoleMessage.getNewComponents().ordinal());
            preparedStatement.setBoolean(10, reactionRoleMessage.getShowRoleNumbers());
            preparedStatement.setBoolean(11, reactionRoleMessage.getShowRoleConnections());
        });

        try {
            MySQLManager.update("DELETE FROM ReactionRolesMessageSlot WHERE messageId = ?;", preparedStatement -> {
                preparedStatement.setLong(1, reactionRoleMessage.getMessageId());
            });
        } catch (SQLException | InterruptedException e) {
            MainLogger.get().error("SQL Exception", e);
        }

        if (!reactionRoleMessage.getSlots().isEmpty()) {
            try (DBBatch batch = new DBBatch("INSERT IGNORE INTO ReactionRolesMessageSlot (messageId, slotId, emoji, roleId) VALUES (?, ?, ?, ?)")) {
                for (int i = 0; i < reactionRoleMessage.getSlots().size(); i++) {
                    ReactionRoleMessageSlot slot = reactionRoleMessage.getSlots().get(i);
                    int finalI = i;
                    batch.add(preparedStatement -> {
                        preparedStatement.setLong(1, reactionRoleMessage.getMessageId());
                        preparedStatement.setInt(2, finalI);

                        if (slot.getEmoji() != null) {
                            preparedStatement.setString(3, slot.getEmoji().getFormatted());
                        } else {
                            preparedStatement.setNull(3, Types.VARCHAR);
                        }

                        preparedStatement.setLong(4, slot.getRoleId());
                    });
                }
                batch.execute();
            } catch (SQLException e) {
                MainLogger.get().error("SQL Exception", e);
            }
        }
    }

    private void removeReactionRoleMessage(ReactionRoleMessage reactionRoleMessage) {
        MySQLManager.asyncUpdate("DELETE FROM ReactionRolesMessage WHERE messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, reactionRoleMessage.getMessageId());
        });
        MySQLManager.asyncUpdate("DELETE FROM ReactionRolesMessageSlot WHERE messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, reactionRoleMessage.getMessageId());
        });
    }

}
