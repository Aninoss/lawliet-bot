package events.discordevents.guildmemberjoin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import commands.Command;
import commands.runnables.utilitycategory.AutoRolesCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.AninossRaidProtection;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBots = true)
public class GuildMemberJoinAutoRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        Locale locale = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getLocale();

        for (Role role : DBAutoRoles.getInstance().retrieve(event.getGuild().getIdLong()).getRoleIds()
                .transform(event.getGuild()::getRoleById, ISnowflake::getIdLong)
        ) {
            if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role)) {
                if (role.getIdLong() != 462410205288726531L ||
                        (AninossRaidProtection.getInstance().check(event.getMember(), role) &&
                                event.getUser().getTimeCreated().toInstant().plus(1, ChronoUnit.HOURS).isBefore(Instant.now()))
                ) {
                    event.getGuild().addRoleToMember(event.getMember(), role)
                            .reason(Command.getCommandLanguage(AutoRolesCommand.class, locale).getTitle())
                            .queue();
                }
            }
        }

        return true;
    }

}
