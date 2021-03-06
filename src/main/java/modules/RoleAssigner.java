package modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.CustomThread;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

    public Optional<CompletableFuture<Boolean>> assignRoles(Guild guild, Role role, boolean add) {
        synchronized (guild) {
            if (busyServers.asMap().containsKey(guild.getIdLong())) return Optional.empty();

            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

            Thread t = new CustomThread(() -> {
                try {
                    Thread.sleep(1000);
                    for (Member member : new ArrayList<>(guild.getMembers())) {
                        if (member.getRoles().contains(role) != add && guild.getMembers().contains(member)) {
                            if (BotPermissionUtil.can(role.getGuild(), Permission.MANAGE_ROLES) && role.getGuild().getSelfMember().canInteract(role)) {
                                if (add) role.getGuild().addRoleToMember(member, role).complete();
                                else role.getGuild().removeRoleFromMember(member, role).complete();
                            }
                        }
                    }
                    completableFuture.complete(true);
                } catch (InterruptedException interruptedException) {
                    //Ignore
                } finally {
                    busyServers.invalidate(guild.getIdLong());
                }
                completableFuture.complete(false);
            }, "role_assignment", 1);
            busyServers.put(guild.getIdLong(), t);
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
