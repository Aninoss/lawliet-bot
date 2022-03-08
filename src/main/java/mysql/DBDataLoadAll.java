package mysql;

import core.ShardManager;

public class DBDataLoadAll<T> extends DBDataLoad<T> {

    public DBDataLoadAll(String table, String requiredAttributes) {
        this(table, requiredAttributes, "");
    }

    public DBDataLoadAll(String table, String requiredAttributes, String add) {
        super(table, requiredAttributes, "(serverId >> 22) % ? >= ? AND (serverId >> 22) % ? <= ?" + add,
                preparedStatement -> {
                    preparedStatement.setInt(1, ShardManager.getTotalShards());
                    preparedStatement.setInt(2, ShardManager.getShardIntervalMin());
                    preparedStatement.setInt(3, ShardManager.getTotalShards());
                    preparedStatement.setInt(4, ShardManager.getShardIntervalMax());
                }
        );
    }

}
