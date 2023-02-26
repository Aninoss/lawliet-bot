package mysql.modules.staticreactionmessages;

import java.sql.Types;
import java.util.Map;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBMapCache;
import mysql.MySQLManager;

public class DBStaticReactionMessages extends DBMapCache<Long, CustomObservableMap<Long, StaticReactionMessageData>> {

    private static final DBStaticReactionMessages ourInstance = new DBStaticReactionMessages();

    public static DBStaticReactionMessages getInstance() {
        return ourInstance;
    }

    private DBStaticReactionMessages() {
    }

    @Override
    protected CustomObservableMap<Long, StaticReactionMessageData> load(Long guildId) throws Exception {
        Map<Long, StaticReactionMessageData> staticReactionMap = new DBDataLoad<StaticReactionMessageData>(
                "StaticReactionMessages",
                "serverId, channelId, messageId, command, secondaryId",
                "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getMap(
                StaticReactionMessageData::getMessageId,
                resultSet -> new StaticReactionMessageData(
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getLong(3),
                        resultSet.getString(4),
                        resultSet.getString(5)
                )
        );

        return new CustomObservableMap<>(staticReactionMap)
                .addMapAddListener(this::addStaticReaction)
                .addMapRemoveListener(this::removeStaticReaction);
    }

    private void addStaticReaction(StaticReactionMessageData staticReactionMessageData) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO StaticReactionMessages (serverId, channelId, messageId, command, secondaryId) VALUES (?,?,?,?,?);", preparedStatement -> {
            preparedStatement.setLong(1, staticReactionMessageData.getGuildId());
            preparedStatement.setLong(2, staticReactionMessageData.getStandardGuildMessageChannelId());
            preparedStatement.setLong(3, staticReactionMessageData.getMessageId());
            preparedStatement.setString(4, staticReactionMessageData.getCommand());

            if (staticReactionMessageData.getSecondaryId() != null) {
                preparedStatement.setString(5, staticReactionMessageData.getSecondaryId());
            } else {
                preparedStatement.setNull(5, Types.VARCHAR);
            }
        });
    }

    private void removeStaticReaction(StaticReactionMessageData staticReactionMessageData) {
        MySQLManager.asyncUpdate("DELETE FROM StaticReactionMessages WHERE messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, staticReactionMessageData.getMessageId());
        });
    }

}
