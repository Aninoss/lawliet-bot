package DiscordEvents.ServerMemberJoin;

import Commands.ManagementCategory.AutoRolesCommand;
import Core.PermissionCheckRuntime;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberJoinAbstract;
import MySQL.Modules.AutoRoles.DBAutoRoles;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import java.util.Locale;

@DiscordEventAnnotation
public class ServerMemberJoinAutoRoles extends ServerMemberJoinAbstract {

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        for (Role role : DBAutoRoles.getInstance().getBean(server.getId()).getRoleIds().transform(server::getRoleById, DiscordEntity::getId)) {
            if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role)) event.getUser().addRole(role).get();
        }

        return true;
    }
    
}
