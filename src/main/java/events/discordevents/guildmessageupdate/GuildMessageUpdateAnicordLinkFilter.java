package events.discordevents.guildmessageupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageUpdateAbstract;
import modules.LinkFilter;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageUpdateAnicordLinkFilter extends GuildMessageUpdateAbstract {

    @Override
    public boolean onGuildMessageUpdate(MessageUpdateEvent event, EntityManagerWrapper entityManager) {
        return LinkFilter.check(event.getMessage());
    }

}
