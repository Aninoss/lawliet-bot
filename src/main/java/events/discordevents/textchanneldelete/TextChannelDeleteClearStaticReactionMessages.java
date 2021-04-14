package events.discordevents.textchanneldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.TextChannelDeleteAbstract;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;

@DiscordEvent
public class TextChannelDeleteClearStaticReactionMessages extends TextChannelDeleteAbstract {

    @Override
    public boolean onTextChannelDelete(TextChannelDeleteEvent event) {
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).values().removeIf(data -> data.getTextChannelId() == event.getChannel().getIdLong());
        return true;
    }

}