package DiscordEvents.ServerMemberJoin;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberJoinAbstract;
import Modules.MemberCountDisplay;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation(allowBannedUser = true)
public class ServerMemberJoinMCDisplays extends ServerMemberJoinAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinMCDisplays.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        MemberCountDisplay.manage(DBServer.getInstance().getBean(event.getServer().getId()).getLocale(), event.getServer());
        return true;
    }
    
}
