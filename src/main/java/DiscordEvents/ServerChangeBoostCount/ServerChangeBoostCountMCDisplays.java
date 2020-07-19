package DiscordEvents.ServerChangeBoostCount;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerChangeBoostCountAbstract;
import Modules.MemberCountDisplay;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerChangeBoostCountEvent;

import java.util.Locale;

@DiscordEventAnnotation()
public class ServerChangeBoostCountMCDisplays extends ServerChangeBoostCountAbstract {

    @Override
    public boolean onServerChangeBoostCount(ServerChangeBoostCountEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        MemberCountDisplay.manage(locale, server);
        return true;
    }

}
