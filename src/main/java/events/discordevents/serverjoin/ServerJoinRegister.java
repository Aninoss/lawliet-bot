package events.discordevents.serverjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.ServerJoinAbstract;
import mysql.modules.server.DBServer;
import org.javacord.api.event.server.ServerJoinEvent;

@DiscordEvent(priority = EventPriority.HIGH)
public class ServerJoinRegister extends ServerJoinAbstract {

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DBServer.getInstance().getBean(event.getServer().getId());
        return true;
    }

}
