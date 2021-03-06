package mysql.modules.bannedusers;

import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

import java.util.ArrayList;

public class DBBannedUsers extends DBSingleCache<BannedUsersBean> {

    private static final DBBannedUsers ourInstance = new DBBannedUsers();

    public static DBBannedUsers getInstance() {
        return ourInstance;
    }

    private DBBannedUsers() {
    }

    @Override
    protected BannedUsersBean loadBean() throws Exception {
        BannedUsersBean bannedUsersBean = new BannedUsersBean(getUserIds());
        bannedUsersBean.getUserIds()
                .addListAddListener(list -> list.forEach(this::addUserId))
                .addListRemoveListener(list -> list.forEach(this::removeUserId));

        return bannedUsersBean;
    }

    private ArrayList<Long> getUserIds() {
        return new DBDataLoad<Long>("BannedUsers", "userId", "1",
                preparedStatement -> {
                }
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

}
