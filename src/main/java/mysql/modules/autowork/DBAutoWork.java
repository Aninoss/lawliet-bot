package mysql.modules.autowork;

import java.util.ArrayList;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

public class DBAutoWork extends DBSingleCache<AutoWorkData> {

    private static final DBAutoWork ourInstance = new DBAutoWork();

    public static DBAutoWork getInstance() {
        return ourInstance;
    }

    private DBAutoWork() {
    }

    @Override
    protected AutoWorkData loadBean() throws Exception {
        ArrayList<Long> autoWorkList = new DBDataLoad<Long>("AutoWork", "userId", "active = 1")
                .getArrayList(resultSet -> resultSet.getLong(1));

        AutoWorkData autoWorkData = new AutoWorkData(autoWorkList);
        autoWorkData.getUserList().addListAddListener(list -> list.forEach(this::addAutoWork))
                .addListRemoveListener(list -> list.forEach(this::removeAutoWork));

        return autoWorkData;
    }

    private void addAutoWork(long userId) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoWork (userId, active) VALUES (?, 1);", preparedStatement -> preparedStatement.setLong(1, userId));
    }

    private void removeAutoWork(long userId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM AutoWork WHERE userId = ?;", preparedStatement -> preparedStatement.setLong(1, userId));
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 5;
    }

}
