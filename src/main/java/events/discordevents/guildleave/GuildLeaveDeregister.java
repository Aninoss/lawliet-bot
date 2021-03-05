package events.discordevents.guildleave;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildLeaveAbstract;
import mysql.modules.server.DBServer;
import org.javacord.api.event.server.ServerLeaveEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildLeaveDeregister extends GuildLeaveAbstract {

    @Override
    public boolean onGuildLeave(ServerLeaveEvent event) throws Throwable {
        DBServer.getInstance().remove(event.getServer().getId());
        return true;
    }

}
