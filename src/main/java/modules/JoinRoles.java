package modules;

import commands.Command;
import commands.runnables.configurationcategory.AutoRolesCommand;
import commands.runnables.configurationcategory.StickyRolesCommand;
import commands.runnables.fisherysettingscategory.FisheryCommand;
import commands.runnables.moderationcategory.JailCommand;
import core.PermissionCheckRuntime;
import core.RestActionQueue;
import core.atomicassets.AtomicRole;
import core.utils.BotPermissionUtil;
import core.utils.TimeUtil;
import modules.fishery.Fishery;
import modules.fishery.FisheryStatus;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.StickyRolesEntity;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.jails.DBJails;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class JoinRoles {

    private static final AninossRaidProtection aninossRaidProtection = new AninossRaidProtection();

    public static boolean guildIsRelevant(Guild guild, GuildEntity guildEntity) {
        return !DBAutoRoles.getInstance().retrieve(guild.getIdLong()).getRoleIds().isEmpty() ||
                (guildEntity.getFishery().getFisheryStatus() == FisheryStatus.ACTIVE && !guildEntity.getFishery().getRoleIds().isEmpty()) ||
                !guildEntity.getStickyRoles().getRoleIds().isEmpty() ||
                !guildEntity.getModeration().getJailRoleIds().isEmpty();
    }

    public static CompletableFuture<Void> process(Member member, boolean bulk, GuildEntity guildEntity) {
        if (member.isPending()) {
            return CompletableFuture.completedFuture(null);
        }

        HashSet<Role> rolesToAdd = new HashSet<>();
        HashSet<Class<? extends Command>> commandClasses = new HashSet<>();
        Locale locale = guildEntity.getLocale();

        if (DBJails.getInstance().retrieve(member.getGuild().getIdLong()).containsKey(member.getIdLong())) {
            getJailRoles(locale, guildEntity, commandClasses, rolesToAdd);
        } else {
            getAutoRoles(locale, member, commandClasses, rolesToAdd);
            getStickyRoles(locale, member, guildEntity, commandClasses, rolesToAdd);
            getFisheryRoles(locale, member, guildEntity, commandClasses, rolesToAdd, new HashSet<>());
        }

        rolesToAdd.removeIf(role -> member.getRoles().contains(role));
        if (rolesToAdd.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        if (bulk) {
            member.getGuild().modifyMemberRoles(member, rolesToAdd, Collections.emptySet())
                    .reason(generateReason(locale, commandClasses))
                    .queue(v -> future.complete(null), future::completeExceptionally);
        } else {
            RestActionQueue restActionQueue = new RestActionQueue();
            for (Role role : rolesToAdd) {
                AuditableRestAction<Void> restAction = member.getGuild().addRoleToMember(member, role)
                        .reason(generateReason(locale, commandClasses));
                restActionQueue.attach(restAction);
            }
            if (restActionQueue.isSet()) {
                restActionQueue.getCurrentRestAction()
                        .queue(v -> future.complete(null), future::completeExceptionally);
            } else {
                future.complete(null);
            }
        }
        return future;
    }

    public static void getAutoRoles(Locale locale, Member member, HashSet<Class<? extends Command>> commandClasses, HashSet<Role> rolesToAdd) {
        Guild guild = member.getGuild();
        for (Role role : DBAutoRoles.getInstance().retrieve(guild.getIdLong()).getRoleIds()
                .transform(guild::getRoleById, ISnowflake::getIdLong)
        ) {
            if (PermissionCheckRuntime.botCanManageRoles(locale, AutoRolesCommand.class, role)) {
                int currentHour = TimeUtil.currentHourOfDay();
                if (role.getIdLong() != 462410205288726531L ||
                        (aninossRaidProtection.check(member, role) &&
                                member.getUser().getTimeCreated().toInstant().plus(1, ChronoUnit.HOURS).isBefore(Instant.now()) &&
                                currentHour >= 6 && currentHour < 23)
                ) {
                    rolesToAdd.add(role);
                    commandClasses.add(AutoRolesCommand.class);
                }
            }
        }
    }

    public static void getFisheryRoles(Locale locale, Member member, GuildEntity guildEntity, HashSet<Class<? extends Command>> commandClasses, HashSet<Role> rolesToAdd, HashSet<Role> rolesToRemove) {
        int rolesToAddSize = rolesToAdd.size();
        int rolesToRemoveSize = rolesToRemove.size();
        Fishery.getFisheryRoles(locale, member, guildEntity, rolesToAdd, rolesToRemove);
        if (rolesToAdd.size() > rolesToAddSize || rolesToRemove.size() > rolesToRemoveSize) {
            commandClasses.add(FisheryCommand.class);
        }
    }

    public static void getJailRoles(Locale locale, GuildEntity guildEntity, HashSet<Class<? extends Command>> commandClasses, HashSet<Role> rolesToAdd) {
        List<Role> jailRoles = AtomicRole.to(guildEntity.getModeration().getJailRoles());
        PermissionCheckRuntime.botCanManageRoles(locale, JailCommand.class, jailRoles);
        for (Role jailRole : jailRoles) {
            if (BotPermissionUtil.canManage(jailRole)) {
                rolesToAdd.add(jailRole);
                commandClasses.add(JailCommand.class);
            }
        }
    }

    public static void getStickyRoles(Locale locale, Member member, GuildEntity guildEntity, HashSet<Class<? extends Command>> commandClasses, HashSet<Role> rolesToAdd) {
        StickyRolesEntity stickyRoles = guildEntity.getStickyRoles();
        for (long activeRoleId : stickyRoles.getActiveRoleIdsForMember(member.getIdLong())) {
            if (!stickyRoles.getRoleIds().contains(activeRoleId)) {
                continue;
            }

            Role role = member.getGuild().getRoleById(activeRoleId);
            if (role == null ||
                    !PermissionCheckRuntime.botCanManageRoles(locale, StickyRolesCommand.class, role)
            ) {
                continue;
            }

            rolesToAdd.add(role);
            commandClasses.add(StickyRolesCommand.class);
        }
    }

    private static String generateReason(Locale locale, HashSet<Class<? extends Command>> commandClasses) {
        StringBuilder sb = new StringBuilder();
        for (Class<? extends Command> clazz : commandClasses) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(Command.getCommandLanguage(clazz, locale).getTitle());
        }
        return sb.toString();
    }

}
