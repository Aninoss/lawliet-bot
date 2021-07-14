package mysql.modules.autoquote;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import mysql.DBMain;
import mysql.DBObserverMapCache;

public class DBAutoQuote extends DBObserverMapCache<Long, AutoQuoteData> {

    private static final DBAutoQuote ourInstance = new DBAutoQuote();

    public static DBAutoQuote getInstance() {
        return ourInstance;
    }

    private DBAutoQuote() {
    }

    @Override
    protected AutoQuoteData load(Long serverId) throws Exception {
        AutoQuoteData autoQuoteBean;

        try (PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active FROM AutoQuote WHERE serverId = ?;")) {
            preparedStatement.setLong(1, serverId);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                autoQuoteBean = new AutoQuoteData(
                        serverId,
                        resultSet.getBoolean(1)
                );
            } else {
                autoQuoteBean = new AutoQuoteData(
                        serverId,
                        true
                );
            }
        }

        return autoQuoteBean;
    }

    @Override
    protected void save(AutoQuoteData serverBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoQuote (serverId, active) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverBean.getGuildId());
            preparedStatement.setBoolean(2, serverBean.isActive());
        });
    }

}
