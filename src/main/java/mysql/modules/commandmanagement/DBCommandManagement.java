package mysql.modules.commandmanagement;

import java.util.ArrayList;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBObserverMapCache;

public class DBCommandManagement extends DBObserverMapCache<Long, CommandManagementData> {

    private static final DBCommandManagement ourInstance = new DBCommandManagement();

    public static DBCommandManagement getInstance() {
        return ourInstance;
    }

    private DBCommandManagement() {
    }

    @Override
    protected CommandManagementData load(Long serverId) throws Exception {
        CommandManagementData commandManagementBean = new CommandManagementData(
                serverId,
                getSwitchedOffElements(serverId)
        );

        commandManagementBean.getSwitchedOffElements()
                .addListAddListener(list -> list.forEach(element -> addSwitchedOffElement(commandManagementBean.getGuildId(), element)))
                .addListRemoveListener(list -> list.forEach(element -> removeSwitchedOffElement(commandManagementBean.getGuildId(), element)));

        return commandManagementBean;
    }

    @Override
    protected void save(CommandManagementData commandManagementBean) {
    }

    private ArrayList<String> getSwitchedOffElements(long serverId) {
        return new DBDataLoad<String>("CMOff", "element", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getString(1));
    }

    private void addSwitchedOffElement(long serverId, String element) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO CMOff (serverId, element) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, element);
        });
    }

    private void removeSwitchedOffElement(long serverId, String element) {
        DBMain.getInstance().asyncUpdate("DELETE FROM CMOff WHERE serverId = ? AND element = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, element);
        });
    }

}
