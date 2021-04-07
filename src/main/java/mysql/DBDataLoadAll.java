package mysql;

import core.ShardManager;

public class DBDataLoadAll<T> extends DBDataLoad<T> {

    public DBDataLoadAll(String table, String requiredAttributes) {
        super(table, requiredAttributes, "(serverId >> 22) % ? >= ? AND (serverId >> 22) % ? <= ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(2, ShardManager.getInstance().getShardIntervalMin());
                    preparedStatement.setInt(3, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(4, ShardManager.getInstance().getShardIntervalMax());
                }
        );
    }

}
