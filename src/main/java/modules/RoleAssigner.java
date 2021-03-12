package modules;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import commands.runnables.utilitycategory.AssignRoleCommand;
import commands.runnables.utilitycategory.RevokeRoleCommand;
import core.MainLogger;
import core.utils.BotPermissionUtil;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class RoleAssigner {

    private static final RoleAssigner ourInstance = new RoleAssigner();

    public static RoleAssigner getInstance() {
        return ourInstance;
    }

    private RoleAssigner() {
    }

    private final Cache<Long, AtomicBoolean> busyServers = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public Optional<CompletableFuture<Boolean>> assignRoles(Role role, boolean add) {
        Guild guild = role.getGuild();
        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();
        synchronized (guild) {
            if (busyServers.asMap().containsKey(guild.getIdLong())) {
                return Optional.empty();
            }

            AtomicBoolean active = new AtomicBoolean(true);
            busyServers.put(guild.getIdLong(), active);
            return Optional.of(CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(500);
                    for (Member member : new ArrayList<>(guild.getMembers())) {
                        if (active.get()) {
                            if (member.getRoles().contains(role) != add &&
                                    guild.getMembers().contains(member) &&
                                    BotPermissionUtil.can(role.getGuild(), Permission.MANAGE_ROLES) &&
                                    role.getGuild().getSelfMember().canInteract(role)
                            ) {
                                if (add) {
                                    role.getGuild().addRoleToMember(member, role)
                                            .reason(Command.getCommandLanguage(AssignRoleCommand.class, locale).getTitle())
                                            .complete();
                                } else {
                                    role.getGuild().removeRoleFromMember(member, role)
                                            .reason(Command.getCommandLanguage(RevokeRoleCommand.class, locale).getTitle())
                                            .complete();
                                }
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
