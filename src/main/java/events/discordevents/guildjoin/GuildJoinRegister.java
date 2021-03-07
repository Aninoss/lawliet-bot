package events.discordevents.guildjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

@DiscordEvent(priority = EventPriority.HIGH)
public class GuildJoinRegister extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event) {
        DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        return true;
    }

}
