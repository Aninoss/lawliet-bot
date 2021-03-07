package events.discordevents.guildleave;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildLeaveAbstract;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildLeaveDeregister extends GuildLeaveAbstract {

    @Override
    public boolean onGuildLeave(GuildLeaveEvent event) throws Throwable {
        DBGuild.getInstance().remove(event.getGuild().getIdLong());
        return true;
    }

}
