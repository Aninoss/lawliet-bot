package MySQL.Modules.InviteTypeUsages;

import Constants.InviteTypes;
import MySQL.DBBeanGenerator;
import MySQL.DBMain;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBInviteTypeUsages extends DBBeanGenerator<InviteTypes, InviteTypeUsagesBean> {

    private static final DBInviteTypeUsages ourInstance = new DBInviteTypeUsages();
    public static DBInviteTypeUsages getInstance() { return ourInstance; }
    private DBInviteTypeUsages() {}

    @Override
    protected InviteTypeUsagesBean loadBean(InviteTypes type) throws Exception {
        InviteTypeUsagesBean inviteTypeUsagesBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT usages FROM InviteTypeUsages WHERE type = ?;");
        preparedStatement.setString(1, type.name());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            inviteTypeUsagesBean = new InviteTypeUsagesBean(
                    type,
                    resultSet.getLong(1)
            );
        } else {
            inviteTypeUsagesBean = new InviteTypeUsagesBean(
                    type,
                    0L
            );
        }

        resultSet.close();
        preparedStatement.close();

        return inviteTypeUsagesBean;
    }

    @Override
    protected void saveBean(InviteTypeUsagesBean InviteTypeUsagesBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO InviteTypeUsages (type, usages) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setString(1, InviteTypeUsagesBean.getType().name());
            preparedStatement.setLong(2, InviteTypeUsagesBean.getValue());
        });
    }

}