package MySQL.AutoQuote;

import MySQL.DBMain;
import MySQL.DBBeanGenerator;
import MySQL.Server.DBServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBAutoQuote extends DBBeanGenerator<Long, AutoQuoteBean> {

    private static DBAutoQuote ourInstance = new DBAutoQuote();
    public static DBAutoQuote getInstance() { return ourInstance; }
    private DBAutoQuote() {}

    @Override
    protected AutoQuoteBean loadBean(Long serverId) throws Exception {
        AutoQuoteBean autoQuoteBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active FROM AutoQuote WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            autoQuoteBean = new AutoQuoteBean(
                    serverId,
                    DBServer.getInstance().getBean(serverId),
                    resultSet.getBoolean(1)
            );
        } else {
            autoQuoteBean = new AutoQuoteBean(
                    serverId,
                    DBServer.getInstance().getBean(serverId),
                    true
            );
        }

        resultSet.close();
        preparedStatement.close();

        return autoQuoteBean;
    }

    @Override
    protected void saveBean(AutoQuoteBean serverBean) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO AutoQuote (serverId, active) VALUES (?, ?);");
        preparedStatement.setLong(1, serverBean.getServerId());
        preparedStatement.setBoolean(2, serverBean.isActive());

        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

}
