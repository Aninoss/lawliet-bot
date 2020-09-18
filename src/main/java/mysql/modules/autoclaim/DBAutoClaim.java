package mysql.modules.autoclaim;

import mysql.DBBeanGenerator;
import mysql.DBMain;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBAutoClaim extends DBBeanGenerator<Long, AutoClaimBean> {

    private static final DBAutoClaim ourInstance = new DBAutoClaim();
    public static DBAutoClaim getInstance() { return ourInstance; }
    private DBAutoClaim() {}

    @Override
    protected AutoClaimBean loadBean(Long userId) throws Exception {
        AutoClaimBean autoClaimBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active FROM AutoClaim WHERE userId = ?;");
        preparedStatement.setLong(1, userId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            autoClaimBean = new AutoClaimBean(
                    userId,
                    resultSet.getBoolean(1)
            );
        } else {
            autoClaimBean = new AutoClaimBean(
                    userId,
                    false
            );
        }

        resultSet.close();
        preparedStatement.close();

        return autoClaimBean;
    }

    @Override
    protected void saveBean(AutoClaimBean autoClaim) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoClaim (userId, active) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, autoClaim.getUserId());
            preparedStatement.setBoolean(2, autoClaim.isActive());
        });
    }

}
