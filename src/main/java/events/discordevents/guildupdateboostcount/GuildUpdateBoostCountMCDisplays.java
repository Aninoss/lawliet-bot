package events.discordevents.guildupdateboostcount;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildUpdateBoostCountAbstract;
import modules.MemberCountDisplay;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerChangeBoostCountEvent;

import java.util.Locale;

@DiscordEvent()
public class GuildUpdateBoostCountMCDisplays extends GuildUpdateBoostCountAbstract {

    @Override
    public boolean onServerChangeBoostCount(ServerChangeBoostCountEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        MemberCountDisplay.getInstance().manage(locale, server);
        return true;
    }

}
