package modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.CustomThread;
import core.MainLogger;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RoleAssigner {

    private static final RoleAssigner ourInstance = new RoleAssigner();

    public static RoleAssigner getInstance() {
        return ourInstance;
    }

    private RoleAssigner() {
    }

    private final Cache<Long, Thread> busyServers = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public Optional<CompletableFuture<Boolean>> assignRoles(Server server, Role role, boolean add) {
        synchronized (server) {
            if (busyServers.asMap().containsKey(server.getId())) return Optional.empty();

            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

            Thread t = new CustomThread(() -> {
                try {
                    Thread.sleep(1000);
                    for (User user : new ArrayList<>(server.getMembers())) {
                        if (role.getUsers().contains(user) != add && server.getMembers().contains(user)) {
                            if (add) user.addRole(role).get();
                            else user.removeRole(role).get();
                        }
                    }
                    completableFuture.complete(true);
                } catch (InterruptedException interruptedException) {
                    //Ignore
                } catch (ExecutionException e) {
                    MainLogger.get().error("Exception in role assignment", e);
                } finally {
                    busyServers.invalidate(server.getId());
                }
                completableFuture.complete(false);
            }, "role_assignment", 1);
            busyServers.put(server.getId(), t);
            t.start();

            return Optional.of(completableFuture);
        }
    }

    public void cancel(long serverId) {
        Thread t = busyServers.getIfPresent(serverId);
        if (t != null) {
            t.interrupt();
        }
    }

}
