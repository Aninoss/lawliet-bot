package events.discordevents.guildmessageupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageUpdateAbstract;
import modules.automod.InviteFilter;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageUpdateInviteFilter extends GuildMessageUpdateAbstract {

    @Override
    public boolean onGuildMessageUpdate(GuildMessageUpdateEvent event) throws Throwable {
        return new InviteFilter(event.getMessage()).check();
    }

}
