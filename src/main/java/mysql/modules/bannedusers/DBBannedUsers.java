package mysql.modules.bannedusers;

import java.util.Map;
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
        BannedUsersData bannedUsersData = new BannedUsersData(getSlots());
        bannedUsersData.getSlotsMap()
                .addMapAddListener(this::addSlot)
                .addMapRemoveListener(this::removeSlot);

        return bannedUsersData;
    }

    private Map<Long, BannedUserSlot> getSlots() {
        return new DBDataLoad<BannedUserSlot>("BannedUsers", "userId, reason", "1")
                .getMap(
                        BannedUserSlot::getUserId,
                        resultSet -> new BannedUserSlot(resultSet.getLong(1), resultSet.getString(2))
                );
    }

    private void addSlot(BannedUserSlot bannedUserSlot) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO BannedUsers (userId, reason) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, bannedUserSlot.getUserId());
            preparedStatement.setString(2, bannedUserSlot.getReason());
        });
    }

    private void removeSlot(BannedUserSlot bannedUserSlot) {
        MySQLManager.asyncUpdate("DELETE FROM BannedUsers WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, bannedUserSlot.getUserId());
        });
    }

}
