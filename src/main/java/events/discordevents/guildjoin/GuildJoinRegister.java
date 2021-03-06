package events.discordevents.guildjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import mysql.modules.server.DBServer;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

@DiscordEvent(priority = EventPriority.HIGH)
public class GuildJoinRegister extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event) {
        DBServer.getInstance().retrieve(event.getGuild().getIdLong());
        return true;
    }

}
