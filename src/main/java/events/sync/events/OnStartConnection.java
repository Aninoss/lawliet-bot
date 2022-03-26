package events.sync.events;

import core.DiscordConnector;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "START_CONNECTION")
public class OnStartConnection implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        int shardMin = jsonObject.getInt("shard_min");
        int shardMax = jsonObject.getInt("shard_max");
        int totalShards = jsonObject.getInt("total_shards");
        DiscordConnector.connect(shardMin, shardMax, totalShards);
        return null;
    }

}
