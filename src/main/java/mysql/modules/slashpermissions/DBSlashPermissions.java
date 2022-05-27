package mysql.modules.slashpermissions;

import java.util.*;
import mysql.DBDataLoad;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

public class DBSlashPermissions extends DBObserverMapCache<Long, SlashPermissionsData> {

    private static final DBSlashPermissions ourInstance = new DBSlashPermissions();

    public static DBSlashPermissions getInstance() {
        return ourInstance;
    }

    private DBSlashPermissions() {
    }

    @Override
    protected SlashPermissionsData load(Long serverId) throws Exception {
        HashMap<String, List<SlashPermissionsSlot>> permissionMap = new HashMap<>();
        List<SlashPermissionsSlot> permissionList = new DBDataLoad<SlashPermissionsSlot>("SlashPermissions", "command, objectId, objectType, allowed", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getList(resultSet -> new SlashPermissionsSlot(
                        serverId,
                        resultSet.getString(1),
                        resultSet.getLong(2),
                        SlashPermissionsSlot.Type.values()[resultSet.getInt(3)],
                        resultSet.getBoolean(4)
                )
        );
        for (SlashPermissionsSlot slashPermissionsSlot : permissionList) {
            ArrayList<SlashPermissionsSlot> commandPermissionList = (ArrayList<SlashPermissionsSlot>) permissionMap
                    .computeIfAbsent(slashPermissionsSlot.getCommand(), k -> new ArrayList<>());
            commandPermissionList.add(slashPermissionsSlot);
        }

        return new SlashPermissionsData(
                serverId,
                Collections.unmodifiableMap(permissionMap)
        );
    }

    @Override
    protected void save(SlashPermissionsData slashPermissionsData) {
        MySQLManager.asyncUpdate("DELETE FROM SlashPermissions WHERE serverId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, slashPermissionsData.getGuildId());
        });

        Map<String, List<SlashPermissionsSlot>> permissionMap = slashPermissionsData.getPermissionMap();
        for (String command : permissionMap.keySet()) {
            for (SlashPermissionsSlot slot : permissionMap.get(command)) {
                MySQLManager.asyncUpdate("INSERT IGNORE INTO SlashPermissions (serverId, command, objectId, objectType, allowed) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
                    preparedStatement.setLong(1, slot.getGuildId());
                    preparedStatement.setString(2, slot.getCommand());
                    preparedStatement.setLong(3, slot.getObjectId());
                    preparedStatement.setInt(4, slot.getType().ordinal());
                    preparedStatement.setBoolean(5, slot.isAllowed());
                });
            }
        }
    }

}
