package DiscordEvents.ServerMemberJoin;

import Commands.ManagementCategory.MemberCountDisplayCommand;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberJoinAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation(allowBannedUser = true)
public class ServerMemberJoinMCDisplays extends ServerMemberJoinAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinMCDisplays.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        MemberCountDisplayCommand.manage(DBServer.getInstance().getBean(event.getServer().getId()).getLocale(), event.getServer());
        return true;
    }
    
}
