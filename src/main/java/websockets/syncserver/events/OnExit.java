package websockets.syncserver.events;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "EXIT")
public class OnExit implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnExit.class);

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        LOGGER.info("EXIT - Received exit signal from sync server");
        System.exit(0);
        return null;
    }

}
