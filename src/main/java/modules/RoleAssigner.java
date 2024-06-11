package modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import core.CustomObservableMap;
import core.MainLogger;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import core.utils.FutureUtil;
import mysql.modules.jails.DBJails;
import mysql.modules.jails.JailData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class RoleAssigner {

    private static final Cache<Long, AtomicBoolean> busyServers = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public static Optional<CompletableFuture<Boolean>> assignRoles(Guild guild, List<Role> roles, boolean add, Locale locale, Class<? extends Command> commandClass) {
        synchronized (guild) {
            if (busyServers.asMap().containsKey(guild.getIdLong())) {
                return Optional.empty();
            }

            AtomicBoolean active = new AtomicBoolean(true);
            busyServers.put(guild.getIdLong(), active);
            return Optional.of(FutureUtil.supplyAsync(() -> {
                try {
                    CustomObservableMap<Long, JailData> jails = DBJails.getInstance().retrieve(guild.getIdLong());
                    MemberCacheController.getInstance().loadMembersFull(guild).join();

                    for (Member member : new ArrayList<>(guild.getMembers())) {
                        if (!active.get()) {
                            return false;
                        }

                        if (jails.containsKey(member.getIdLong())) {
                            JailData jailData = jails.get(member.getIdLong());
                            HashSet<Long> previousRoleIds = new HashSet<>(jailData.getPreviousRoleIds());
                            if (add) {
                                roles.forEach(r -> previousRoleIds.add(r.getIdLong()));
                            } else {
                                roles.forEach(r -> previousRoleIds.remove(r.getIdLong()));
                            }

                            JailData newJailData = new JailData(guild.getIdLong(), member.getIdLong(), jailData.getExpirationTime().orElse(null), new ArrayList<>(previousRoleIds));
                            jails.put(newJailData.getMemberId(), newJailData);

                            if (add) {
                                continue;
                            }
                        }

                        MemberCacheController.getInstance().loadMembersFull(guild).join();
                        boolean canInteract = roles.stream().allMatch(role ->
                                BotPermissionUtil.can(role.getGuild(), Permission.MANAGE_ROLES) &&
                                        BotPermissionUtil.canManage(role)
                        );

                        if (guild.getMembers().contains(member) && canInteract) {
                            guild.modifyMemberRoles(member, add ? roles : Collections.emptyList(), add ? Collections.emptyList() : roles)
                                    .reason(Command.getCommandLanguage(commandClass, locale).getTitle())
                                    .complete();
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

    public static void cancel(long guildId) {
        AtomicBoolean atomicBoolean = busyServers.getIfPresent(guildId);
        if (atomicBoolean != null) {
            atomicBoolean.set(false);
        }
    }

}
