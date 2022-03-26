package events.sync.events;

import core.ShardManager;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "BLOCK_SHARDS")
public class OnBlockShards implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        int totalShards = jsonObject.getInt("total_shards");
        int shardsMin = jsonObject.getInt("shards_min");
        int shardsMax = jsonObject.getInt("shards_max");
        ShardManager.getJDABlocker().add(totalShards, shardsMin, shardsMax);
        return null;
    }

}
