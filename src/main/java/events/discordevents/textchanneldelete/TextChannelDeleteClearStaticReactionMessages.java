package events.discordevents.textchanneldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.TextChannelDeleteAbstract;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

@DiscordEvent
public class TextChannelDeleteClearStaticReactionMessages extends TextChannelDeleteAbstract {

    @Override
    public boolean onTextChannelDelete(ChannelDeleteEvent event) {
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                .values()
                .removeIf(data -> data.getBaseGuildMessageChannelId() == event.getChannel().getIdLong());
        return true;
    }

}