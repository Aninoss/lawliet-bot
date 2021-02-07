package websockets.syncserver;

import core.Bot;
import core.DiscordApiManager;
import org.javacord.api.util.logging.ExceptionLogger;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.CustomWebSocketClient;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class SyncManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(SyncManager.class);

    private static final SyncManager ourInstance = new SyncManager();

    public static SyncManager getInstance() {
        return ourInstance;
    }

    private final CustomWebSocketClient client;
    private boolean started = false;

    private SyncManager() {
        try {
            client = new CustomWebSocketClient(
                    System.getenv("SYNC_HOST"),
                    Integer.parseInt(System.getenv("SYNC_PORT")),
                    "cluster_" + Bot.getClusterId(),
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
                        LOGGER.error("Error when creating sync event class", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof SyncServerFunction)
                .map(obj -> (SyncServerFunction) obj)
                .forEach(this::addEvent);
    }

    public synchronized void start() {
        if (started)
            return;
        started = true;

        this.client.connect();
    }

    public CustomWebSocketClient getClient() {
        return client;
    }

    public void setFullyConnected() {
        getClient().addHeader("already_connected", "true");
        getClient().addHeader("shard_min", String.valueOf(DiscordApiManager.getInstance().getShardIntervalMin()));
        getClient().addHeader("shard_max", String.valueOf(DiscordApiManager.getInstance().getShardIntervalMax()));
        getClient().addHeader("total_shards", String.valueOf(DiscordApiManager.getInstance().getTotalShards()));
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
        if (event != null)
            this.client.addEventHandler(event.event(), function);
    }

}
