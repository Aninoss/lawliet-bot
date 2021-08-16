package mysql.modules.autoquote;

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
        return DBMain.getInstance().get(
                "SELECT active FROM AutoQuote WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        return new AutoQuoteData(
                                serverId,
                                resultSet.getBoolean(1)
                        );
                    } else {
                        return new AutoQuoteData(
                                serverId,
                                false
                        );
                    }
                }
        );
    }

    @Override
    protected void save(AutoQuoteData serverBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoQuote (serverId, active) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverBean.getGuildId());
            preparedStatement.setBoolean(2, serverBean.isActive());
        });
    }

}
