package events.sync.events;

import core.HeartbeatReceiver;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import org.json.JSONObject;

@SyncServerEvent(event = "HEARTBEAT")
public class OnHeartbeat implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        HeartbeatReceiver.registerHeartbeat();
        return null;
    }

}
