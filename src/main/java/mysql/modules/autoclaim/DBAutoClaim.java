package mysql.modules.autoclaim;

import java.util.ArrayList;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

public class DBAutoClaim extends DBSingleCache<AutoClaimData> {

    private static final DBAutoClaim ourInstance = new DBAutoClaim();

    public static DBAutoClaim getInstance() {
        return ourInstance;
    }

    private DBAutoClaim() {
    }

    @Override
    protected AutoClaimData loadBean() throws Exception {
        ArrayList<Long> autoClaimList = new DBDataLoad<Long>("AutoClaim", "userId", "active = 1")
                .getArrayList(resultSet -> resultSet.getLong(1));

        AutoClaimData autoClaimData = new AutoClaimData(autoClaimList);
        autoClaimData.getUserList().addListAddListener(list -> list.forEach(this::addAutoClaim))
                .addListRemoveListener(list -> list.forEach(this::removeAutoClaim));

        return autoClaimData;
    }

    private void addAutoClaim(long userId) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoClaim (userId, active) VALUES (?, 1);", preparedStatement -> preparedStatement.setLong(1, userId));
    }

    private void removeAutoClaim(long userId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM AutoClaim WHERE userId = ?;", preparedStatement -> preparedStatement.setLong(1, userId));
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 5;
    }

}
