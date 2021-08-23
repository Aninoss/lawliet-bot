package websockets.syncserver;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import core.*;
import org.java_websocket.client.WebSocketJsonClient;
import org.reflections.Reflections;

public class SyncManager extends Startable {

    private static final SyncManager ourInstance = new SyncManager();

    public static SyncManager getInstance() {
        return ourInstance;
    }

    private final WebSocketJsonClient client;

    private SyncManager() {
        try {
            client = new WebSocketJsonClient(
                    System.getenv("SYNC_HOST"),
                    Integer.parseInt(System.getenv("SYNC_PORT")),
                    "cluster_" + Program.getClusterId(),
                    System.getenv("SYNC_AUTH"),
                    getSocketClientHeaders()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Reflections reflections = new Reflections("websockets/syncserver/events");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(SyncServerEvent.class);
        annotated.stream()
                .map(clazz -> {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        MainLogger.get().error("Error when creating sync event class", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof SyncServerFunction)
                .map(obj -> (SyncServerFunction) obj)
                .forEach(this::addEvent);
    }

    @Override
    protected void run() {
        this.client.connect();
    }

    public WebSocketJsonClient getClient() {
        return client;
    }

    public void setFullyConnected() {
        getClient().addHeader("already_connected", "true");
        getClient().addHeader("shard_min", String.valueOf(ShardManager.getInstance().getShardIntervalMin()));
        getClient().addHeader("shard_max", String.valueOf(ShardManager.getInstance().getShardIntervalMax()));
        getClient().addHeader("total_shards", String.valueOf(ShardManager.getInstance().getTotalShards()));
        SendEvent.sendFullyConnected().exceptionally(ExceptionLogger.get());
    }

    private HashMap<String, String> getSocketClientHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("size", String.valueOf(Math.round(Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0))));
        headers.put("already_connected", "false");
        return headers;
    }

    private void addEvent(SyncServerFunction function) {
        SyncServerEvent event = function.getClass().getAnnotation(SyncServerEvent.class);
        if (event != null) {
            this.client.addEventHandler(event.event(), function);
        }
    }

    public void reconnect() {
        client.reconnect();
    }

}
