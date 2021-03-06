package websockets.syncserver.events;

import core.MainLogger;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "EXIT")
public class OnExit implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        MainLogger.get().info("EXIT - Received exit signal from sync server");
        System.exit(0);
        return null;
    }

}
