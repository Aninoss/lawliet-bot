package mysql.modules.staticreactionmessages;

import java.sql.Types;
import java.util.Map;
import commands.Command;
import commands.runnables.utilitycategory.ReactionRolesCommand;
import core.CustomObservableMap;
import core.ShardManager;
import mysql.DBDataLoad;
import mysql.DBMapCache;
import mysql.MySQLManager;
import mysql.modules.reactionroles.DBReactionRoles;
import net.dv8tion.jda.api.entities.Guild;

public class DBStaticReactionMessages extends DBMapCache<Long, CustomObservableMap<Long, StaticReactionMessageData>> {

    private static final DBStaticReactionMessages ourInstance = new DBStaticReactionMessages();

    public static DBStaticReactionMessages getInstance() {
        return ourInstance;
    }

    private DBStaticReactionMessages() {
    }

    @Override
    protected CustomObservableMap<Long, StaticReactionMessageData> load(Long guildId) throws Exception {
        Map<Long, StaticReactionMessageData> map = new DBDataLoad<StaticReactionMessageData>(
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

        CustomObservableMap<Long, StaticReactionMessageData> staticReactionMap = new CustomObservableMap<>(map)
                .addMapAddListener(this::addStaticReaction)
                .addMapRemoveListener(this::removeStaticReaction);

        Guild guild = ShardManager.getLocalGuildById(guildId).get();
        for (long messageId : map.keySet()) {
            StaticReactionMessageData staticReactionMessageData = map.get(messageId);
            if (guild.getGuildChannelById(staticReactionMessageData.getStandardGuildMessageChannelId()) == null) {
                staticReactionMap.remove(messageId);
            }
        }

        return staticReactionMap;
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
        }).thenAccept(n -> {
            Runnable runAfterSave = staticReactionMessageData.getRunAfterSave();
            if (runAfterSave != null) {
                runAfterSave.run();
            }
        });
    }

    private void removeStaticReaction(StaticReactionMessageData staticReactionMessageData) {
        if (staticReactionMessageData.getCommand().equals(Command.getCommandProperties(ReactionRolesCommand.class).trigger())) {
            DBReactionRoles.getInstance().retrieve(staticReactionMessageData.getGuildId())
                    .remove(staticReactionMessageData.getMessageId());
        }
        MySQLManager.asyncUpdate("DELETE FROM StaticReactionMessages WHERE messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, staticReactionMessageData.getMessageId());
        });
    }

}
