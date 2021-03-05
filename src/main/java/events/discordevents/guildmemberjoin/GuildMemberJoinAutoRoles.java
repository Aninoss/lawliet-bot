package events.discordevents.guildmemberjoin;

import commands.runnables.utilitycategory.AutoRolesCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.AninossRaidProtection;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@DiscordEvent(allowBots = true)
public class GuildMemberJoinAutoRoles extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        for (Role role : DBAutoRoles.getInstance().getBean(server.getId()).getRoleIds().transform(server::getRoleById, DiscordEntity::getId)) {
            if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role)) {
                if (role.getId() != 462410205288726531L || (AninossRaidProtection.getInstance().check(event.getUser(), role) && event.getUser().getCreationTimestamp().plus(1, ChronoUnit.HOURS).isBefore(Instant.now()))) {
                    event.getUser().addRole(role).exceptionally(ExceptionLogger.get());
                }
            }
        }

        return true;
    }
    
}
