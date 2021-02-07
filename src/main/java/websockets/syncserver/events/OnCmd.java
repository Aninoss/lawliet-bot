package websockets.syncserver.events;

import core.Console;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "CMD")
public class OnCmd implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        Console.getInstance().processInput(jsonObject.getString("input"));
        return null;
    }

}
