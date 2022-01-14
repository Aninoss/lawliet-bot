package mysql.modules.stickyroles;

import java.util.List;
import mysql.DBDataLoad;
import mysql.DBMapCache;
import mysql.MySQLManager;

public class DBStickyRoles extends DBMapCache<Long, StickyRolesData> {

    private static final DBStickyRoles ourInstance = new DBStickyRoles();

    public static DBStickyRoles getInstance() {
        return ourInstance;
    }

    private DBStickyRoles() {
    }

    @Override
    protected StickyRolesData load(Long serverId) throws Exception {
        StickyRolesData stickyRolesData = new StickyRolesData(
                serverId,
                getRoleIds(serverId),
                getActions(serverId)
        );

        stickyRolesData.getRoleIds()
                .addListAddListener(list -> list.forEach(roleId -> addRoleId(stickyRolesData.getGuildId(), roleId)))
                .addListRemoveListener(list -> list.forEach(roleId -> removeRoleId(stickyRolesData.getGuildId(), roleId)));
        stickyRolesData.getActions()
                .addListAddListener(list -> list.forEach(this::addAction))
                .addListRemoveListener(list -> list.forEach(this::removeAction));

        return stickyRolesData;
    }

    private List<StickyRolesActionData> getActions(long serverId) {
        return new DBDataLoad<StickyRolesActionData>("StickyRolesActions", "userId, roleId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> new StickyRolesActionData(
                serverId,
                resultSet.getLong(1),
                resultSet.getLong(2)
        ));
    }

    private void addAction(StickyRolesActionData actionData) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO StickyRolesActions (serverId, userId, roleId) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, actionData.getGuildId());
            preparedStatement.setLong(2, actionData.getMemberId());
            preparedStatement.setLong(3, actionData.getRoleId());
        });
    }

    private void removeAction(StickyRolesActionData actionData) {
        MySQLManager.asyncUpdate("DELETE FROM StickyRolesRoles WHERE serverId = ? AND userId = ? AND roleId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, actionData.getGuildId());
            preparedStatement.setLong(2, actionData.getMemberId());
            preparedStatement.setLong(3, actionData.getRoleId());
        });
    }

    private List<Long> getRoleIds(long serverId) {
        return new DBDataLoad<Long>("StickyRolesRoles", "roleId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> resultSet.getLong(1));
    }

    private void addRoleId(long serverId, long roleId) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO StickyRolesRoles (serverId, roleId) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

    private void removeRoleId(long serverId, long roleId) {
        MySQLManager.asyncUpdate("DELETE FROM StickyRolesRoles WHERE serverId = ? AND roleId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, roleId);
        });
    }

}
