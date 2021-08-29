package mysql.modules.bannedwords;

import java.util.ArrayList;
import mysql.DBDataLoad;
import mysql.MySQLManager;
import mysql.DBObserverMapCache;

public class DBBannedWords extends DBObserverMapCache<Long, BannedWordsData> {

    private static final DBBannedWords ourInstance = new DBBannedWords();

    public static DBBannedWords getInstance() {
        return ourInstance;
    }

    private DBBannedWords() {
    }

    @Override
    protected BannedWordsData load(Long serverId) throws Exception {
        BannedWordsData bannedWordsBean = MySQLManager.get(
                "SELECT active FROM BannedWords WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        return new BannedWordsData(
                                serverId,
                                resultSet.getBoolean(1),
                                getIgnoredUsers(serverId),
                                getLogReceivers(serverId),
                                getWords(serverId)
                        );
                    } else {
                        return new BannedWordsData(
                                serverId,
                                false,
                                getIgnoredUsers(serverId),
                                getLogReceivers(serverId),
                                getWords(serverId)
                        );
                    }
                });

        bannedWordsBean.getIgnoredUserIds()
                .addListAddListener(list -> list.forEach(userId -> addIgnoredUser(bannedWordsBean.getGuildId(), userId)))
                .addListRemoveListener(list -> list.forEach(userId -> removeIgnoredUser(bannedWordsBean.getGuildId(), userId)));
        bannedWordsBean.getLogReceiverUserIds()
                .addListAddListener(list -> list.forEach(userId -> addLogReceiver(bannedWordsBean.getGuildId(), userId)))
                .addListRemoveListener(list -> list.forEach(userId -> removeLogReceiver(bannedWordsBean.getGuildId(), userId)));
        bannedWordsBean.getWords()
                .addListAddListener(list -> list.forEach(word -> addWord(bannedWordsBean.getGuildId(), word)))
                .addListRemoveListener(list -> list.forEach(word -> removeWord(bannedWordsBean.getGuildId(), word)));

        return bannedWordsBean;
    }

    @Override
    protected void save(BannedWordsData bannedWordsBean) {
        MySQLManager.asyncUpdate("REPLACE INTO BannedWords (serverId, active) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, bannedWordsBean.getGuildId());
            preparedStatement.setBoolean(2, bannedWordsBean.isActive());
        });
    }

    private ArrayList<Long> getIgnoredUsers(long serverId) {
        return new DBDataLoad<Long>("BannedWordsIgnoredUsers", "userId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addIgnoredUser(long serverId, long userId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO BannedWordsIgnoredUsers (serverId, userId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private void removeIgnoredUser(long serverId, long userId) {
        MySQLManager.asyncUpdate("DELETE FROM BannedWordsIgnoredUsers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private ArrayList<Long> getLogReceivers(long serverId) {
        return new DBDataLoad<Long>("BannedWordsLogRecievers", "userId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addLogReceiver(long serverId, long userId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO BannedWordsLogRecievers (serverId, userId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private void removeLogReceiver(long serverId, long userId) {
        MySQLManager.asyncUpdate("DELETE FROM BannedWordsLogRecievers WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
        });
    }

    private ArrayList<String> getWords(long serverId) {
        return new DBDataLoad<String>("BannedWordsWords", "word", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getString(1));
    }

    private void addWord(long serverId, String word) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO BannedWordsWords (serverId, word) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, word);
        });
    }

    private void removeWord(long serverId, String word) {
        MySQLManager.asyncUpdate("DELETE FROM BannedWordsWords WHERE serverId = ? AND word = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, word);
        });
    }

}
