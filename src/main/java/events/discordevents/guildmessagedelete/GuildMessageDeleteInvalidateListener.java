package events.discordevents.guildmessagedelete;

import commands.CommandContainer;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageDeleteAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;

@DiscordEvent
public class GuildMessageDeleteInvalidateListener extends GuildMessageDeleteAbstract {

    @Override
    public boolean onGuildMessageDelete(MessageDeleteEvent event, EntityManagerWrapper entityManager) {
        CommandContainer.deregisterListeners(event.getMessageIdLong());
        return true;
    }

}
