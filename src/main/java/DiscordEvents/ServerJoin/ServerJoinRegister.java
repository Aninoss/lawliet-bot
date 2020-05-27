package DiscordEvents.ServerJoin;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventPriority;
import DiscordEvents.EventTypeAbstracts.ServerJoinAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.ServerJoinEvent;

@DiscordEventAnnotation(priority = EventPriority.HIGH)
public class ServerJoinRegister extends ServerJoinAbstract {

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DBServer.getInstance().getBean(event.getServer().getId());
        return true;
    }

}
