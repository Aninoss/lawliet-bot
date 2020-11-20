package mysql.modules.bannedusers;

import mysql.DBCached;
import mysql.DBDataLoad;
import mysql.DBMain;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBBannedUsers extends DBCached {

    private static final DBBannedUsers ourInstance = new DBBannedUsers();
    public static DBBannedUsers getInstance() { return ourInstance; }
    private DBBannedUsers() {}

    private BannedUsersBean bannedUsersBean = null;

    public synchronized BannedUsersBean getBean() {
        try {
            if (bannedUsersBean == null) {
                bannedUsersBean = new BannedUsersBean(getUserIds());
                bannedUsersBean.getUserIds()
                        .addListAddListener(list -> list.forEach(this::addUserId))
                        .addListRemoveListener(list -> list.forEach(this::removeUserId));
            }

            return bannedUsersBean;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<Long> getUserIds() throws SQLException {
        return new DBDataLoad<Long>("BannedUsers", "userId", "1",
                preparedStatement -> {}
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addUserId(long userId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO BannedUsers (userId) VALUES (?);", preparedStatement -> {
            preparedStatement.setLong(1, userId);
        });
    }

    private void removeUserId(long userId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM BannedUsers WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, userId);
        });
    }

    @Override
    public void clear() {
        bannedUsersBean = null;
    }

}
