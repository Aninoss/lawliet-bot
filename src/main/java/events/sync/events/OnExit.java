package events.sync.events;

import core.MainLogger;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "EXIT")
public class OnExit implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        MainLogger.get().info("EXIT - Received exit signal from sync server");
        System.exit(7);
        return null;
    }

}
