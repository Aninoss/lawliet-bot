package MySQL.Modules.BannedWords;

import MySQL.DBDataLoad;
import MySQL.DBMain;
import MySQL.DBBeanGenerator;
import MySQL.Modules.Server.DBServer;

import java.sql.*;
import java.util.ArrayList;

public class DBBannedWords extends DBBeanGenerator<Long, BannedWordsBean> {

    private static DBBannedWords ourInstance = new DBBannedWords();
    public static DBBannedWords getInstance() { return ourInstance; }
    private DBBannedWords() {}

    @Override
    protected BannedWordsBean loadBean(Long serverId) throws Exception {
        BannedWordsBean bannedWordsBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active FROM BannedWords WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            bannedWordsBean = new BannedWordsBean(
                    serverId,
                    DBServer.getInstance().getBean(serverId),
                    resultSet.getBoolean(1),
                    getIgnoredUsers(serverId),
                    getLogReceivers(serverId),
                    getWords(serverId)
            );
        } else {
            bannedWordsBean = new BannedWordsBean(
                    serverId,
                    DBServer.getInstance().getBean(serverId),
                    false,
                    getIgnoredUsers(serverId),
                    getLogReceivers(serverId),
                    getWords(serverId)
            );
        }

        resultSet.close();
        preparedStatement.close();

        bannedWordsBean.getIgnoredUserIds()
                .addListAddListener(list -> list.forEach(userId -> addIgnoredUser(bannedWordsBean.getServerId(), userId)))
                .addListRemoveListener(list -> list.forEach(userId -> removeIgnoredUser(bannedWordsBean.getServerId(), userId)));
        bannedWordsBean.getLogReceiverUserIds()
                .addListAddListener(list -> list.forEach(userId -> addLogReceiver(bannedWordsBean.getServerId(), userId)))
                .addListRemoveListener(list -> list.forEach(userId -> removeLogReceiver(bannedWordsBean.getServerId(), userId)));
        bannedWordsBean.getWords()
                .addListAddListener(list -> list.forEach(word -> addWord(bannedWordsBean.getServerId(), word )))
                .addListRemoveListener(list -> list.forEach(word -> removeWord(bannedWordsBean.getServerId(), word)));

        return bannedWordsBean;
    }

    @Override
    protected void saveBean(BannedWordsBean bannedWordsBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO BannedWords (serverId, active) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, bannedWordsBean.getServerId());
            preparedStatement.setBoolean(2, bannedWordsBean.isActive());
        });
    }

    private ArrayList<Long> getIgnoredUsers(long serverId) throws SQLException {
        return new DBDataLoad<Long>("BannedWordsIgnoredUsers", "userId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addIgnoredUser(long serverId, long userId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO BannedWordsIgnoredUsers (serverId, userId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private void removeIgnoredUser(long serverId, long userId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM BannedWordsIgnoredUsers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private ArrayList<Long> getLogReceivers(long serverId) throws SQLException {
        return new DBDataLoad<Long>("BannedWordsLogRecievers", "userId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addLogReceiver(long serverId, long userId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO BannedWordsLogRecievers (serverId, userId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private void removeLogReceiver(long serverId, long userId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM BannedWordsLogRecievers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private ArrayList<String> getWords(long serverId) throws SQLException {
        return new DBDataLoad<String>("BannedWordsWords", "word", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getString(1));
    }

    private void addWord(long serverId, String word) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO BannedWordsWords (serverId, word) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, word);
        });
    }

    private void removeWord(long serverId, String word) {
        DBMain.getInstance().asyncUpdate("DELETE FROM BannedWordsWords WHERE serverId = ? AND word = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, word);
        });
    }

}
