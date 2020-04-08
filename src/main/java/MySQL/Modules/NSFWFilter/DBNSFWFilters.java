package MySQL.Modules.NSFWFilter;

import MySQL.DBBeanGenerator;
import MySQL.DBDataLoad;
import MySQL.DBMain;
import MySQL.Modules.Server.DBServer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBNSFWFilters extends DBBeanGenerator<Long, NSFWFiltersBean> {

    private static DBNSFWFilters ourInstance = new DBNSFWFilters();
    public static DBNSFWFilters getInstance() { return ourInstance; }
    private DBNSFWFilters() {}

    @Override
    protected NSFWFiltersBean loadBean(Long serverId) throws Exception {
        NSFWFiltersBean nsfwFiltersBean = new NSFWFiltersBean(
                serverId,
                DBServer.getInstance().getBean(serverId),
                getKeywords(serverId)
        );

        nsfwFiltersBean.getKeywords()
                .addListAddListener(list -> list.forEach(keyword -> addKeyword(serverId, keyword)))
                .addListRemoveListener(list -> list.forEach(keyword -> removeKeyword(serverId, keyword)));

        return nsfwFiltersBean;
    }

    @Override
    protected void saveBean(NSFWFiltersBean nsfwFiltersBean) {}

    private ArrayList<String> getKeywords(long serverId) throws SQLException {
        return new DBDataLoad<String>("NSFWFilter", "keyword", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getString(1));
    }

    private void addKeyword(long serverId, String keyword) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO NSFWFilter (serverId, keyword) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, keyword);
        });
    }

    private void removeKeyword(long serverId, String keyword) {
        DBMain.getInstance().asyncUpdate("DELETE FROM NSFWFilter WHERE serverId = ? AND keyword = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, keyword);
        });
    }

}
