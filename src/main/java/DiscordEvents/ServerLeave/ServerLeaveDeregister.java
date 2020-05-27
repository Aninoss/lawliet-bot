package DiscordEvents.ServerLeave;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventPriority;
import DiscordEvents.EventTypeAbstracts.ServerLeaveAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.ServerLeaveEvent;

@DiscordEventAnnotation(priority = EventPriority.LOW)
public class ServerLeaveDeregister extends ServerLeaveAbstract {

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        DBServer.getInstance().remove(event.getServer().getId());
        return true;
    }

}
