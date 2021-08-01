package modules;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import commands.Command;
import commands.runnables.utilitycategory.AutoRolesCommand;
import core.PermissionCheckRuntime;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class AutoRoles {

    public static void process(Member member) {
        Guild guild = member.getGuild();
        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();
        HashSet<Role> rolesToAdd = new HashSet<>();

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

        if (rolesToAdd.size() > 0) {
            guild.modifyMemberRoles(member, rolesToAdd, Collections.emptySet())
                    .reason(Command.getCommandLanguage(AutoRolesCommand.class, locale).getTitle())
                    .queue();
        }
    }

}
