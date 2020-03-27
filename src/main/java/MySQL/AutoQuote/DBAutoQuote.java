package MySQL.AutoQuote;

import MySQL.DBMain;
import MySQL.Server.DBServer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;

public class DBAutoQuote implements Observer {

    private static DBAutoQuote ourInstance = new DBAutoQuote();
    public static DBAutoQuote getInstance() { return ourInstance; }
    private DBAutoQuote() {}

    private LoadingCache<Long, AutoQuoteBean> cache = CacheBuilder.newBuilder().build(
            new CacheLoader<Long, AutoQuoteBean>() {
            @Override
                public AutoQuoteBean load(@NonNull Long serverId) throws SQLException, ExecutionException {
                    AutoQuoteBean autoQuoteBean;

                    PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active FROM AutoQuote WHERE serverId = ?;");
                    preparedStatement.setLong(1, serverId);
                    preparedStatement.execute();

                    ResultSet resultSet = preparedStatement.getResultSet();
                    if (resultSet.next()) {
                        autoQuoteBean = new AutoQuoteBean(
                                serverId,
                                DBServer.getInstance().getServerBean(serverId),
                                resultSet.getBoolean(1)
                        );
                    } else {
                        autoQuoteBean = new AutoQuoteBean(
                                serverId,
                                DBServer.getInstance().getServerBean(serverId),
                                true
                        );
                    }

                    resultSet.close();
                    preparedStatement.close();

                    autoQuoteBean.addObserver(DBAutoQuote.getInstance());
                    return autoQuoteBean;
                }
            });

    public AutoQuoteBean getAutoQuoteBean(long serverId) throws ExecutionException {
        return cache.get(serverId);
    }

    @Override
    public void update(@NonNull Observable o, Object arg) {
        try {
            saveBean((AutoQuoteBean) o);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveBean(AutoQuoteBean serverBean) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO AutoQuote (serverId, active) VALUES (?, ?);");
        preparedStatement.setLong(1, serverBean.getServerId());
        preparedStatement.setBoolean(2, serverBean.isActive());

        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

}
