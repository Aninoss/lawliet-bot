package events.sync;

import java.net.URI;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import core.MainLogger;
import core.ShardManager;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.reflections.Reflections;

public class EventManager {

    private static final HashMap<String, SyncServerFunction> eventMap = new HashMap<>();

    public static void register() {
        registerEvents();
        registerRestService();
        heartbeat();
    }

    public static SyncServerFunction getEvent(String name) {
        return eventMap.get(name);
    }

    private static void heartbeat() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            SendEvent.sendHeartbeat(
                    System.getenv("SYNC_OWN_IP"),
                    ShardManager.isEverythingConnected(),
                    ShardManager.getShardIntervalMin(),
                    ShardManager.getShardIntervalMax(),
                    ShardManager.getTotalShards()
            );
        }, 3, 3, TimeUnit.SECONDS);
    }

    private static void registerEvents() {
        Reflections reflections = new Reflections("events/sync/events");
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
                .forEach(EventManager::addEvent);
    }

    private static void registerRestService() {
        ResourceConfig rc = new ResourceConfig(RestService.class, AuthFilter.class);

        URI endpoint = UriBuilder
                .fromUri("http://0.0.0.0/api/")
                .port(Integer.parseInt(System.getenv("SYNC_SERVER_PORT")))
                .build();

        GrizzlyHttpServerFactory.createHttpServer(endpoint, rc);
    }

    private static void addEvent(SyncServerFunction function) {
        SyncServerEvent event = function.getClass().getAnnotation(SyncServerEvent.class);
        if (event != null) {
            eventMap.put(event.event(), function);
        }
    }

}
