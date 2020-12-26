package websockets.syncserver.events;

import core.DiscordConnector;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "START_CONNECTION")
public class OnStartConnection implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnStartConnection.class);

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        int shardMin = jsonObject.getInt("shard_min");
        int shardMax = jsonObject.getInt("shard_max");
        int totalShards = jsonObject.getInt("total_shards");
        DiscordConnector.getInstance().connect(shardMin, shardMax, totalShards);
        return null;
    }

}
