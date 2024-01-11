package mysql;

import core.Program;
import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;

public class DBDataLoadAll<T> extends DBDataLoad<T> {

    public DBDataLoadAll(String table, String requiredAttributes) {
        this(table, requiredAttributes, "");
    }

    public DBDataLoadAll(String table, String requiredAttributes, String add) {
        if (Program.publicInstance()) {
            init(table, requiredAttributes, "(serverId >> 22) % ? >= ? AND (serverId >> 22) % ? <= ?" + add,
                    preparedStatement -> {
                        preparedStatement.setInt(1, ShardManager.getTotalShards());
                        preparedStatement.setInt(2, ShardManager.getShardIntervalMin());
                        preparedStatement.setInt(3, ShardManager.getTotalShards());
                        preparedStatement.setInt(4, ShardManager.getShardIntervalMax());
                    }
            );
        } else {
            List<Guild> guilds = ShardManager.getLocalGuilds();
            init(table, requiredAttributes, guilds.isEmpty() ? "0" : ("(" + "serverId = ? OR ".repeat(guilds.size()) + "0)" + add),
                    preparedStatement -> {
                        for (int i = 0; i < guilds.size(); i++) {
                            preparedStatement.setLong(i + 1, guilds.get(i).getIdLong());
                        }
                    }
            );
        }
    }

}
