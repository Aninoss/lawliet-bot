package events.discordevents.guildjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import mysql.modules.server.DBServer;
import org.javacord.api.event.server.ServerJoinEvent;

@DiscordEvent(priority = EventPriority.HIGH)
public class GuildJoinRegister extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(ServerJoinEvent event) throws Throwable {
        DBServer.getInstance().retrieve(event.getServer().getId());
        return true;
    }

}
