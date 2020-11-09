package events.discordevents.serverchangeboostcount;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerChangeBoostCountAbstract;
import modules.MemberCountDisplay;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerChangeBoostCountEvent;

import java.util.Locale;

@DiscordEvent()
public class ServerChangeBoostCountMCDisplays extends ServerChangeBoostCountAbstract {

    @Override
    public boolean onServerChangeBoostCount(ServerChangeBoostCountEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        MemberCountDisplay.getInstance().manage(locale, server);
        return true;
    }

}
