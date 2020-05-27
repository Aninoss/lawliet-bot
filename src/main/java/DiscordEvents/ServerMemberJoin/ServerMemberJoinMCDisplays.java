package DiscordEvents.ServerMemberJoin;

import Commands.ManagementCategory.MemberCountDisplayCommand;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberJoinAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;

@DiscordEventAnnotation
public class ServerMemberJoinMCDisplays extends ServerMemberJoinAbstract {

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        MemberCountDisplayCommand.manage(DBServer.getInstance().getBean(event.getServer().getId()).getLocale(), event.getServer());
        return true;
    }
    
}
