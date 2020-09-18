package events.discordevents.servermemberjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerMemberJoinAbstract;
import modules.MemberCountDisplay;
import mysql.modules.server.DBServer;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class ServerMemberJoinMCDisplays extends ServerMemberJoinAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinMCDisplays.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        MemberCountDisplay.manage(DBServer.getInstance().getBean(event.getServer().getId()).getLocale(), event.getServer());
        return true;
    }
    
}
