package websockets.webcomserver;

import core.CustomThread;
import org.java_websocket.WebSocket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public abstract class EventAbstract implements BiConsumer<WebSocket, JSONObject> {

    private final static Logger LOGGER = LoggerFactory.getLogger(EventAbstract.class);

    private final WebComServer webComServer;
    private final String event;

    public EventAbstract(WebComServer webComServer, String event) {
        this.webComServer = webComServer;
        this.event = event;
    }

    @Override
    public void accept(WebSocket webSocket, JSONObject mainJSON) {
        new CustomThread(() -> {
            try {
                JSONObject response = processData(mainJSON, webComServer);

                if (response != null) {
                    response.put("id", mainJSON.getString("id"));
                    WebComServer.getInstance().send(webSocket, event, response);
                }
            } catch (Exception e) {
                LOGGER.error("Error on web socket event", e);
            }
        }, "webcom_event").start(); //TODO DEBUG
    }

    protected abstract JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception;

}
