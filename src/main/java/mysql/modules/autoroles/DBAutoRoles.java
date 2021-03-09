package mysql.modules.autoroles;

import java.util.ArrayList;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBAutoRoles extends DBMapCache<Long, AutoRolesBean> {

    private static final DBAutoRoles ourInstance = new DBAutoRoles();

    public static DBAutoRoles getInstance() {
        return ourInstance;
    }

    private DBAutoRoles() {
    }

    @Override
    protected AutoRolesBean load(Long serverId) throws Exception {
        AutoRolesBean autoRolesBean = new AutoRolesBean(
                serverId,
                getRoleIds(serverId)
        );

        autoRolesBean.getRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addRoleId(autoRolesBean.getGuildId(), roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeRoleId(autoRolesBean.getGuildId(), roleId)));

        return autoRolesBean;
    }

    @Override
    protected void save(AutoRolesBean autoRolesBean) {
    }

    private ArrayList<Long> getRoleIds(long serverId) {
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
