package modules;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import commands.runnables.fisherysettingscategory.FisheryCommand;
import commands.runnables.moderationcategory.MuteCommand;
import commands.runnables.utilitycategory.AutoRolesCommand;
import constants.FisheryStatus;
import core.PermissionCheckRuntime;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import mysql.modules.moderation.DBModeration;
import mysql.modules.servermute.DBServerMute;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class JoinRoles {

    public static void process(Member member) {
        if (!member.isPending()) {
            HashSet<Role> rolesToAdd = new HashSet<>();
            HashSet<Role> rolesToRemove = new HashSet<>();

            Locale locale = DBGuild.getInstance().retrieve(member.getGuild().getIdLong()).getLocale();
            getAutoRoles(locale, member, rolesToAdd);
            getFisheryRoles(locale, member, rolesToAdd, rolesToRemove);
            getMuteRoles(locale, member, rolesToAdd);

            if (rolesToAdd.size() > 0 || rolesToRemove.size() > 0) {
                member.getGuild().modifyMemberRoles(member, rolesToAdd, rolesToRemove)
                        .queue();
            }
        }
    }

    public static void getAutoRoles(Locale locale, Member member, HashSet<Role> rolesToAdd) {
        Guild guild = member.getGuild();
        for (Role role : DBAutoRoles.getInstance().retrieve(guild.getIdLong()).getRoleIds()
                .transform(guild::getRoleById, ISnowflake::getIdLong)
        ) {
            if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role)) {
                if (role.getIdLong() != 462410205288726531L ||
                        (AninossRaidProtection.getInstance().check(member, role) &&
                                member.getUser().getTimeCreated().toInstant().plus(1, ChronoUnit.HOURS).isBefore(Instant.now()))
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
            if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, FisheryCommand.class, role) && give != member.getRoles().contains(role)) {
                if (give) {
                    rolesToAdd.add(role);
                } else {
                    rolesToRemove.add(role);
                }
            }
        }
    }

    public static void getMuteRoles(Locale locale, Member member, HashSet<Role> rolesToAdd) {
        Guild guild = member.getGuild();
        if (DBServerMute.getInstance().retrieve(guild.getIdLong()).containsKey(member.getIdLong())) {
            DBModeration.getInstance().retrieve(guild.getIdLong()).getMuteRole().ifPresent(muteRole -> {
                if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, MuteCommand.class, muteRole)) {
                    rolesToAdd.add(muteRole);
                }
            });
        }
    }

}
