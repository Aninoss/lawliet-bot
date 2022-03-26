package events.sync.events;

import core.Console;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "CMD")
public class OnCmd implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        Console.processInput(jsonObject.getString("input"));
        return null;
    }

}
