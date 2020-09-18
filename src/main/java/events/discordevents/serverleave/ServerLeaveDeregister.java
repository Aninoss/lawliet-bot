package events.discordevents.serverleave;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.ServerLeaveAbstract;
import mysql.modules.server.DBServer;
import org.javacord.api.event.server.ServerLeaveEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class ServerLeaveDeregister extends ServerLeaveAbstract {

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        DBServer.getInstance().remove(event.getServer().getId());
        return true;
    }

}
