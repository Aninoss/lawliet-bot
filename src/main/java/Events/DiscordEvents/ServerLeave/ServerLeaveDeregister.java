package Events.DiscordEvents.ServerLeave;

import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventPriority;
import Events.DiscordEvents.EventTypeAbstracts.ServerLeaveAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.ServerLeaveEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class ServerLeaveDeregister extends ServerLeaveAbstract {

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        DBServer.getInstance().remove(event.getServer().getId());
        return true;
    }

}
