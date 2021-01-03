package websockets;

import core.CustomThread;
import core.GlobalCachedThreadPool;
import core.schedule.MainScheduler;
import core.utils.ExceptionUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomWebSocketClient extends WebSocketClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomWebSocketClient.class);
    private final Random r = new Random();

    private final HashMap<String, Function<JSONObject, JSONObject>> eventHandlers = new HashMap<>();
    private final ArrayList<Supplier<Boolean>> connectedHandlers = new ArrayList<>();
    private final HashMap<Integer, CompletableFuture<JSONObject>> outCache = new HashMap<>();
    private boolean connected = false;

    public CustomWebSocketClient(String host, int port, String socketId, HashMap<String, String> httpHeaders) throws URISyntaxException {
        super(new URI(String.format("ws://%s:%d", host, port)), httpHeaders);
        addHeader("socket_id", socketId);
    }

    public CustomWebSocketClient(String host, int port, String socketId) throws URISyntaxException {
        super(new URI(String.format("ws://%s:%d", host, port)));
        addHeader("socket_id", socketId);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.info("Web socket connected");
        connected = true;
        connectedHandlers.removeIf(Supplier::get);
    }

    public void addConnectedHandler(Supplier<Boolean> supplier) {
        connectedHandlers.add(supplier);
    }

    public void removeConnectedHandler(Supplier<Boolean> supplier) {
        connectedHandlers.remove(supplier);
    }

    public void addEventHandler(String event, Function<JSONObject, JSONObject> eventFunction) {
        eventHandlers.put(event, eventFunction);
    }

    public void removeEventHandler(String event) {
        eventHandlers.remove(event);
    }

    public void removeEventHandler(String event, Function<JSONObject, JSONObject> eventFunction) {
        eventHandlers.remove(event, eventFunction);
    }

    @Override
    public void onMessage(String message) {
        String event = message.split("::")[0];
        JSONObject contentJson = new JSONObject(message.substring(event.length() + 2));

        int requestId = contentJson.getInt("request_id");
        contentJson.remove("request_id");

        if (outCache.containsKey(requestId)) {
            outCache.remove(requestId)
                    .complete(contentJson);
        } else {
            Function<JSONObject, JSONObject> eventFunction = eventHandlers.get(event);
            if (eventFunction != null) {
                Thread t = new CustomThread(() -> {
                    JSONObject responseJson = eventFunction.apply(contentJson);
                    if (responseJson == null) responseJson = new JSONObject();
                    responseJson.put("request_id", requestId);
                    send(event + "::" + responseJson.toString());
                }, "websocket_" + event);

                MainScheduler.getInstance().schedule(5, ChronoUnit.SECONDS, "websocket_" + event + "_observer", () -> {
                    if (t.isAlive()) {
                        Exception e = ExceptionUtil.generateForStack(t);
                        LOGGER.error("websocket_" + event + " took too long to respond!", e);
                    }
                });
                t.start();
            }
        }
    }

    public CompletableFuture<JSONObject> sendSecure(String event, JSONObject content) {
        if (isConnected()) {
            return send(event, content);
        } else {
            CompletableFuture<JSONObject> future = new CompletableFuture<>();
            addConnectedHandler(() -> {
                send(event, content)
                        .exceptionally(e -> {
                            future.completeExceptionally(e);
                            return null;
                        })
                        .thenAccept(future::complete);
                return true;
            });
            return future;
        }
    }

    public synchronized CompletableFuture<JSONObject> send(String event, JSONObject content) {
        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        int id = r.nextInt();

        content.put("request_id", id);
        try {
            send(event + "::" + content.toString());
        } catch (Throwable e) {
            future.completeExceptionally(e);
            return future;
        }
        outCache.put(id, future);

        MainScheduler.getInstance().schedule(5, ChronoUnit.SECONDS, "websocket_" + event, () -> {
            if (outCache.containsKey(id)) {
                outCache.remove(id);
                future.completeExceptionally(new SocketTimeoutException("No response"));
            }
        });

        return future;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (connected) LOGGER.info("Web socket disconnected");
        connected = false;
        GlobalCachedThreadPool.getExecutorService().submit(() -> {
            try {
                Thread.sleep(2000);
                reconnect();
            } catch (InterruptedException e) {
                //Ignore
            }
        });
    }

    @Override
    public void onError(Exception ex) {
        if (!ex.toString().contains("Connection refused"))
            LOGGER.error("Web socket error", ex);
    }

    public boolean isConnected() {
        return connected;
    }

}
