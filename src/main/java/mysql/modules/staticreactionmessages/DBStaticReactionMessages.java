package mysql.modules.staticreactionmessages;

import java.util.HashMap;
import core.CustomObservableMap;
import core.ShardManager;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

public class DBStaticReactionMessages extends DBSingleCache<CustomObservableMap<Long, StaticReactionMessageData>> {

    private static final DBStaticReactionMessages ourInstance = new DBStaticReactionMessages();

    public static DBStaticReactionMessages getInstance() {
        return ourInstance;
    }

    private DBStaticReactionMessages() {
    }

    @Override
    protected CustomObservableMap<Long, StaticReactionMessageData> loadBean() throws Exception {
        HashMap<Long, StaticReactionMessageData> staticReactionMap = new DBDataLoad<StaticReactionMessageData>("StaticReactionMessages", "serverId, channelId, messageId, command", "(serverId >> 22) % ? >= ? AND (serverId >> 22) % ? <= ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(2, ShardManager.getInstance().getShardIntervalMin());
                    preparedStatement.setInt(3, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(4, ShardManager.getInstance().getShardIntervalMax());
                }
        ).getHashMap(
                StaticReactionMessageData::getMessageId,
                resultSet -> new StaticReactionMessageData(
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getLong(3),
                        resultSet.getString(4)
                )
        );

        return new CustomObservableMap<>(staticReactionMap)
                .addMapAddListener(this::addStaticReaction)
                .addMapRemoveListener(this::removeStaticReaction);
    }

    private void addStaticReaction(StaticReactionMessageData staticReactionMessageData) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO StaticReactionMessages (serverId, channelId, messageId, command) VALUES (?,?,?,?);", preparedStatement -> {
            preparedStatement.setLong(1, staticReactionMessageData.getGuildId());
            preparedStatement.setLong(2, staticReactionMessageData.getTextChannelId());
            preparedStatement.setLong(3, staticReactionMessageData.getMessageId());
            preparedStatement.setString(4, staticReactionMessageData.getCommand());
        });
    }

    private void removeStaticReaction(StaticReactionMessageData staticReactionMessageData) {
        DBMain.getInstance().asyncUpdate("DELETE FROM StaticReactionMessages WHERE messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, staticReactionMessageData.getMessageId());
        });
    }

}
