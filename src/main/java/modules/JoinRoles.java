package modules;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import commands.runnables.fisherysettingscategory.FisheryCommand;
import commands.runnables.moderationcategory.JailCommand;
import commands.runnables.moderationcategory.MuteCommand;
import commands.runnables.utilitycategory.AutoRolesCommand;
import commands.runnables.utilitycategory.StickyRolesCommand;
import core.PermissionCheckRuntime;
import core.RestActionQueue;
import core.utils.BotPermissionUtil;
import core.utils.TimeUtil;
import modules.fishery.FisheryStatus;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.jails.DBJails;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import mysql.modules.stickyroles.DBStickyRoles;
import mysql.modules.stickyroles.StickyRolesActionData;
import mysql.modules.stickyroles.StickyRolesData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public class JoinRoles {

    private static final AninossRaidProtection aninossRaidProtection = new AninossRaidProtection();

    public static boolean guildIsRelevant(Guild guild) {
        FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(guild.getIdLong());
        ModerationData moderationData = DBModeration.getInstance().retrieve(guild.getIdLong());
        return DBAutoRoles.getInstance().retrieve(guild.getIdLong()).getRoleIds().size() > 0 ||
                (fisheryGuildData.getGuildData().getFisheryStatus() == FisheryStatus.ACTIVE && fisheryGuildData.getRoleIds().size() > 0) ||
                DBStickyRoles.getInstance().retrieve(guild.getIdLong()).getRoleIds().size() > 0 ||
                moderationData.getJailRoleIds().size() > 0 ||
                moderationData.getMuteRoleId().isPresent();
    }

    public static CompletableFuture<Void> process(Member member, boolean bulk, GuildEntity guildEntity) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!member.isPending()) {
            HashSet<Role> rolesToAdd = new HashSet<>();
            Locale locale = guildEntity.getLocale();

            if (DBJails.getInstance().retrieve(member.getGuild().getIdLong()).containsKey(member.getIdLong())) {
                getJailRoles(locale, member, rolesToAdd);
            } else {
                getAutoRoles(locale, member, rolesToAdd);
                getStickyRoles(locale, member, rolesToAdd);
                getFisheryRoles(locale, member, rolesToAdd, new HashSet<>());
            }
            getMuteRole(locale, member, rolesToAdd);

            rolesToAdd.removeIf(role -> member.getRoles().contains(role));
            if (rolesToAdd.size() > 0) {
                if (bulk) {
                    member.getGuild().modifyMemberRoles(member, rolesToAdd, Collections.emptySet())
                            .queue(v -> future.complete(null), future::completeExceptionally);
                } else {
                    RestActionQueue restActionQueue = new RestActionQueue();
                    for (Role role : rolesToAdd) {
                        AuditableRestAction<Void> restAction = member.getGuild().addRoleToMember(member, role);
                        restActionQueue.attach(restAction);
                    }
                    if (restActionQueue.isSet()) {
                        restActionQueue.getCurrentRestAction()
                                .queue(v -> future.complete(null), future::completeExceptionally);
                    } else {
                        future.complete(null);
                    }
                }
            } else {
                future.complete(null);
            }
        } else {
            future.complete(null);
        }
        return future;
    }

    public static void getAutoRoles(Locale locale, Member member, HashSet<Role> rolesToAdd) {
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
                }
            }
        }
    }

    public static void getFisheryRoles(Locale locale, Member member, HashSet<Role> rolesToAdd, HashSet<Role> rolesToRemove) {
        Guild guild = member.getGuild();
        FisheryGuildData fisheryGuildBean = DBFishery.getInstance().retrieve(guild.getIdLong());
        if (fisheryGuildBean.getGuildData().getFisheryStatus() == FisheryStatus.STOPPED) {
            return;
        }

        List<Role> memberRoles = fisheryGuildBean.getMemberData(member.getIdLong()).getRoles();
        for (Role role : fisheryGuildBean.getRoles()) {
            boolean give = memberRoles.contains(role);
            if (PermissionCheckRuntime.botCanManageRoles(locale, FisheryCommand.class, role) && give != member.getRoles().contains(role)) {
                if (give) {
                    rolesToAdd.add(role);
                } else {
                    rolesToRemove.add(role);
                }
            }
        }
    }

    public static void getMuteRole(Locale locale, Member member, HashSet<Role> rolesToAdd) {
        Guild guild = member.getGuild();
        ServerMuteData serverMuteData = DBServerMute.getInstance().retrieve(guild.getIdLong()).get(member.getIdLong());
        if (serverMuteData != null && !serverMuteData.isNewMethod()) {
            DBModeration.getInstance().retrieve(guild.getIdLong()).getMuteRole().ifPresent(muteRole -> {
                if (PermissionCheckRuntime.botCanManageRoles(locale, MuteCommand.class, muteRole)) {
                    rolesToAdd.add(muteRole);
                }
            });
        }
    }

    public static void getJailRoles(Locale locale, Member member, HashSet<Role> rolesToAdd) {
        Guild guild = member.getGuild();
        List<Role> jailRoles = DBModeration.getInstance().retrieve(guild.getIdLong()).getJailRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong);
        PermissionCheckRuntime.botCanManageRoles(locale, JailCommand.class, jailRoles);
        for (Role jailRole : jailRoles) {
            if (BotPermissionUtil.canManage(jailRole)) {
                rolesToAdd.add(jailRole);
            }
        }
    }

    public static void getStickyRoles(Locale locale, Member member, HashSet<Role> rolesToAdd) {
        Guild guild = member.getGuild();
        StickyRolesData stickyRolesData =  DBStickyRoles.getInstance().retrieve(guild.getIdLong());
        for (StickyRolesActionData actionData : stickyRolesData.getActions()) {
            if (actionData != null &&
                    actionData.getMemberId() == member.getIdLong() &&
                    stickyRolesData.getRoleIds().contains(actionData.getRoleId())
            ) {
                actionData.getRole().ifPresent(role -> {
                    if (PermissionCheckRuntime.botCanManageRoles(locale, StickyRolesCommand.class, role)) {
                        rolesToAdd.add(role);
                    }
                });
            }
        }
    }

}
