package MySQL.Modules.AutoRoles;

import MySQL.DBDataLoad;
import MySQL.DBMain;
import MySQL.DBBeanGenerator;
import MySQL.Modules.Server.DBServer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBAutoRoles extends DBBeanGenerator<Long, AutoRolesBean> {

    private static DBAutoRoles ourInstance = new DBAutoRoles();
    public static DBAutoRoles getInstance() { return ourInstance; }
    private DBAutoRoles() {}

    @Override
    protected AutoRolesBean loadBean(Long serverId) throws Exception {
        AutoRolesBean autoRolesBean = new AutoRolesBean(
                serverId,
                DBServer.getInstance().getBean(serverId),
                getRoleIds(serverId)
        );

        autoRolesBean.getRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addRoleId(autoRolesBean.getServerId(), roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeRoleId(autoRolesBean.getServerId(), roleId)));

        return autoRolesBean;
    }

    @Override
    protected void saveBean(AutoRolesBean autoRolesBean) {}

    private ArrayList<Long> getRoleIds(long serverId) throws SQLException {
        return new DBDataLoad<Long>("BasicRole", "roleId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addRoleId(long serverId, long roleId) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO BasicRole (serverId, roleId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private void removeRoleId(long serverId, long roleId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM BasicRole WHERE serverId = ? AND roleId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

}
