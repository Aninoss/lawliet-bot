package events.discordevents.servermemberleave;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerMemberLeaveAbstract;
import modules.MemberCountDisplay;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import java.util.Locale;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class ServerMemberLeaveMCDisplays extends ServerMemberLeaveAbstract {

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        MemberCountDisplay.manage(locale, server);
        return true;
    }

}
