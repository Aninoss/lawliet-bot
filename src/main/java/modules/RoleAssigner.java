package modules;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import commands.runnables.utilitycategory.AssignRoleCommand;
import commands.runnables.utilitycategory.RevokeRoleCommand;
import core.MainLogger;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import core.utils.FutureUtil;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public class RoleAssigner {

    private final Cache<Long, AtomicBoolean> busyServers = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public Optional<CompletableFuture<Boolean>> assignRoles(Guild guild, List<Role> roles, boolean add) {
        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();
        synchronized (guild) {
            if (busyServers.asMap().containsKey(guild.getIdLong())) {
                return Optional.empty();
            }

            AtomicBoolean active = new AtomicBoolean(true);
            busyServers.put(guild.getIdLong(), active);
            return Optional.of(FutureUtil.supplyAsync(() -> {
                try {
                    Thread.sleep(500);
                    MemberCacheController.getInstance().loadMembersFull(guild).join();
                    for (Member member : new ArrayList<>(guild.getMembers())) {
                        if (active.get()) {
                            MemberCacheController.getInstance().loadMembersFull(guild).join();
                            boolean canInteract = roles.stream().allMatch(role ->
                                    BotPermissionUtil.can(role.getGuild(), Permission.MANAGE_ROLES) &&
                                            role.getGuild().getSelfMember().canInteract(role)
                            );

                            if (guild.getMembers().contains(member) && canInteract) {
                                AuditableRestAction<Void> restAction;
                                if (add) {
                                    restAction = guild.modifyMemberRoles(member, roles, Collections.emptyList())
                                            .reason(Command.getCommandLanguage(AssignRoleCommand.class, locale).getTitle());
                                } else {
                                    restAction = guild.modifyMemberRoles(member, Collections.emptyList(), roles)
                                            .reason(Command.getCommandLanguage(RevokeRoleCommand.class, locale).getTitle());
                                }
                                restAction.complete();
                            }
                        } else {
                            return false;
                        }
                    }
                    return true;
                } catch (Throwable e) {
                    MainLogger.get().error("Error", e);
                } finally {
                    busyServers.invalidate(guild.getIdLong());
                }
                return false;
            }));
        }
    }

    public void cancel(long guildId) {
        AtomicBoolean atomicBoolean = busyServers.getIfPresent(guildId);
        if (atomicBoolean != null) {
            atomicBoolean.set(false);
        }
    }

}
