package websockets.webcomserver;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class CustomWebSocketServer extends WebSocketServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomWebSocketServer.class);
    
    private final HashMap<String, BiConsumer<WebSocket, JSONObject>> eventHandlers = new HashMap<>();
    private final ArrayList<Runnable> connectedHanlders = new ArrayList<>();

    public CustomWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        LOGGER.info("Web socket connected");
        connectedHanlders.forEach(Runnable::run);
    }

    public void addConnectedHandler(Runnable runnable) {
        connectedHanlders.add(runnable);
    }

    public void removeConnectedHandler(Runnable runnable) {
        connectedHanlders.remove(runnable);
    }

    public void addEventHandler(String event, BiConsumer<WebSocket, JSONObject> eventConsumer) {
        eventHandlers.put(event, eventConsumer);
    }

    public void removeEventHandler(String event) {
        eventHandlers.remove(event);
    }

    public void removeEventHandler(String event, BiConsumer<WebSocket, JSONObject> eventConsumer) {
        eventHandlers.remove(event, eventConsumer);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        LOGGER.info("Web socket disconnected");
    }

    public void send(WebSocket webSocket, String event, JSONObject content) {
        webSocket.send(event + "::" + content.toString());
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        String event = message.split("::")[0];
        String content = message.substring(event.length() + 2);
        BiConsumer<WebSocket, JSONObject> eventConsumer = eventHandlers.get(event);
        if (eventConsumer != null)
            eventConsumer.accept(webSocket, new JSONObject(content));
    }

    @Override
    public void onError(WebSocket webSocket, Exception ex) {
        LOGGER.error("Web socket error", ex);
    }

    @Override
    public void onStart() {
        //Ignore
    }

}
