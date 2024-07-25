package mysql.modules.suggestions;

import modules.suggestions.SuggestionMessage;
import mysql.DBDataLoad;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

import java.sql.Types;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class DBSuggestions extends DBObserverMapCache<Long, SuggestionsData> {

    private static final DBSuggestions ourInstance = new DBSuggestions();

    public static DBSuggestions getInstance() {
        return ourInstance;
    }

    private DBSuggestions() {
    }

    @Override
    protected SuggestionsData load(Long serverId) throws Exception {
        SuggestionsData suggestionsBean = MySQLManager.get(
                "SELECT active, channelId FROM SuggestionConfig WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        return new SuggestionsData(
                                serverId,
                                resultSet.getBoolean(1),
                                resultSet.getLong(2),
                                getSuggestionMessages(serverId)
                        );
                    } else {
                        return new SuggestionsData(
                                serverId,
                                false,
                                null,
                                Collections.emptyMap()
                        );
                    }
                }
        );

        suggestionsBean.getSuggestionMessages()
                .addMapAddListener(this::addSuggestionMessage)
                .addMapRemoveListener(this::removeSuggestionMessage);

        return suggestionsBean;
    }

    @Override
    protected void save(SuggestionsData suggestionsBean) {
        MySQLManager.asyncUpdate("REPLACE INTO SuggestionConfig (serverId, active, channelId) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, suggestionsBean.getGuildId());
            preparedStatement.setBoolean(2, suggestionsBean.isActive());

            Optional<Long> channelIdOpt = suggestionsBean.getChannelId();
            if (channelIdOpt.isPresent()) {
                preparedStatement.setLong(3, channelIdOpt.get());
            } else {
                preparedStatement.setNull(3, Types.BIGINT);
            }
        });
    }

    private Map<Long, SuggestionMessage> getSuggestionMessages(long serverId) {
        return new DBDataLoad<SuggestionMessage>("SuggestionMessages", "messageId, userId, content, author", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getMap(
                SuggestionMessage::getMessageId,
                resultSet -> new SuggestionMessage(
                        serverId,
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getString(3),
                        resultSet.getString(4)
                )
        );
    }

    private void addSuggestionMessage(SuggestionMessage suggestionMessage) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO SuggestionMessages (serverId, messageId, userId, content, author) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, suggestionMessage.getGuildId());
            preparedStatement.setLong(2, suggestionMessage.getMessageId());

            if (suggestionMessage.getUserId() != null) {
                preparedStatement.setLong(3, suggestionMessage.getUserId());
            } else {
                preparedStatement.setNull(3, Types.BIGINT);
            }

            preparedStatement.setString(4, suggestionMessage.getContent());

            if (suggestionMessage.getAuthor() != null) {
                preparedStatement.setString(5, suggestionMessage.getAuthor());
            } else {
                preparedStatement.setNull(5, Types.VARCHAR);
            }
        });
    }

    private void removeSuggestionMessage(SuggestionMessage suggestionMessage) {
        MySQLManager.asyncUpdate("DELETE FROM SuggestionMessages WHERE serverId = ? AND messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, suggestionMessage.getGuildId());
            preparedStatement.setLong(2, suggestionMessage.getMessageId());
        });
    }

}
