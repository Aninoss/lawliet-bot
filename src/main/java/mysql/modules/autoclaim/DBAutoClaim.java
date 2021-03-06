package mysql.modules.autoclaim;

import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

import java.util.ArrayList;

public class DBAutoClaim extends DBSingleCache<AutoClaimBean> {

    private static final DBAutoClaim ourInstance = new DBAutoClaim();

    public static DBAutoClaim getInstance() {
        return ourInstance;
    }

    private DBAutoClaim() {
    }

    @Override
    protected AutoClaimBean loadBean() throws Exception {
        ArrayList<Long> autoClaimList = new DBDataLoad<Long>("AutoClaim", "userId", "active = 1",
                preparedStatement -> {
                }
        ).getArrayList(resultSet -> resultSet.getLong(1));

        AutoClaimBean autoClaimBean = new AutoClaimBean(autoClaimList);
        autoClaimBean.getUserList().addListAddListener(list -> list.forEach(this::addAutoClaim))
                .addListRemoveListener(list -> list.forEach(this::removeAutoClaim));

        return autoClaimBean;
    }

    private void addAutoClaim(long userId) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoClaim (userId, active) VALUES (?, 1);", preparedStatement -> {
            preparedStatement.setLong(1, userId);
        });
    }

    private void removeAutoClaim(long userId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM AutoClaim WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, userId);
        });
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 5;
    }

}
