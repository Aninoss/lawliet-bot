package mysql.modules.suggestions;

import modules.suggestions.SuggestionMessage;
import mysql.DBBeanGenerator;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.modules.server.DBServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Optional;

public class DBSuggestions extends DBBeanGenerator<Long, SuggestionsBean> {

    private static final DBSuggestions ourInstance = new DBSuggestions();
    public static DBSuggestions getInstance() { return ourInstance; }
    private DBSuggestions() {}

    @Override
    protected SuggestionsBean loadBean(Long serverId) throws Exception {
        SuggestionsBean suggestionsBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active, channelId FROM SuggestionConfig WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            suggestionsBean = new SuggestionsBean(
                    DBServer.getInstance().getBean(serverId),
                    resultSet.getBoolean(1),
                    resultSet.getLong(2),
                    getSuggestionMessages(serverId)
            );
        } else {
            suggestionsBean = new SuggestionsBean(
                    DBServer.getInstance().getBean(serverId),
                    false,
                    null,
                    new ArrayList<>()
            );
        }

        resultSet.close();
        preparedStatement.close();

        suggestionsBean.getSuggestionMessages()
                .addListAddListener(list -> list.forEach(this::addSuggestionMessage))
                .addListRemoveListener(list -> list.forEach(this::removeSuggestionMessage));

        return suggestionsBean;
    }

    @Override
    protected void saveBean(SuggestionsBean suggestionsBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO SuggestionConfig (serverId, active, channelId) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, suggestionsBean.getServerId());
            preparedStatement.setBoolean(2, suggestionsBean.isActive());

            Optional<Long> channelIdOpt = suggestionsBean.getChannelId();
            if (channelIdOpt.isPresent()) preparedStatement.setLong(3, channelIdOpt.get());
            else preparedStatement.setNull(3, Types.BIGINT);
        });
    }

    private ArrayList<SuggestionMessage> getSuggestionMessages(long serverId) {
        return new DBDataLoad<SuggestionMessage>("SuggestionMessages", "messageId, content", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> new SuggestionMessage(
                serverId,
                resultSet.getLong(1),
                resultSet.getString(2)
        ));
    }

    private void addSuggestionMessage(SuggestionMessage suggestionMessage) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO SuggestionMessages (serverId, messageId, content) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, suggestionMessage.getServerId());
            preparedStatement.setLong(2, suggestionMessage.getMessageId());
            preparedStatement.setString(3, suggestionMessage.getContent());
        });
    }

    private void removeSuggestionMessage(SuggestionMessage suggestionMessage) {
        DBMain.getInstance().asyncUpdate("DELETE FROM SuggestionMessages WHERE serverId = ? AND messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, suggestionMessage.getServerId());
            preparedStatement.setLong(2, suggestionMessage.getMessageId());
        });
    }

}