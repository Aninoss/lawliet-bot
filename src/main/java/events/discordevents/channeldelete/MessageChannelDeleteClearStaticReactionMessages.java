package events.discordevents.channeldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.MessageChannelDeleteAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

@DiscordEvent
public class MessageChannelDeleteClearStaticReactionMessages extends MessageChannelDeleteAbstract {

    @Override
    public boolean onMessageChannelDelete(ChannelDeleteEvent event, EntityManagerWrapper entityManager) {
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                .values()
                .removeIf(data -> data.getGuildMessageChannelId() == event.getChannel().getIdLong());
        return true;
    }

}