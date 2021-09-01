package mysql.modules.bannedusers;

import java.util.List;
import mysql.DBDataLoad;
import mysql.DBSingleCache;
import mysql.MySQLManager;

public class DBBannedUsers extends DBSingleCache<BannedUsersData> {

    private static final DBBannedUsers ourInstance = new DBBannedUsers();

    public static DBBannedUsers getInstance() {
        return ourInstance;
    }

    private DBBannedUsers() {
    }

    @Override
    protected BannedUsersData loadBean() throws Exception {
        BannedUsersData bannedUsersData = new BannedUsersData(getUserIds());
        bannedUsersData.getUserIds()
                .addListAddListener(list -> list.forEach(this::addUserId))
                .addListRemoveListener(list -> list.forEach(this::removeUserId));

        return bannedUsersData;
    }

    private List<Long> getUserIds() {
        return new DBDataLoad<Long>("BannedUsers", "userId", "1")
                .getList(resultSet -> resultSet.getLong(1));
    }

    private void addUserId(long userId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO BannedUsers (userId) VALUES (?);", preparedStatement -> {
            preparedStatement.setLong(1, userId);
        });
    }

    private void removeUserId(long userId) {
        MySQLManager.asyncUpdate("DELETE FROM BannedUsers WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, userId);
        });
    }

}
