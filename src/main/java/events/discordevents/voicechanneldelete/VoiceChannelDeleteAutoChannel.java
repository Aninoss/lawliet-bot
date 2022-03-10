package events.discordevents.voicechanneldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelDeleteAbstract;
import mysql.modules.autochannel.DBAutoChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

@DiscordEvent
public class VoiceChannelDeleteAutoChannel extends VoiceChannelDeleteAbstract {

    @Override
    public boolean onVoiceChannelDelete(ChannelDeleteEvent event) {
        DBAutoChannel.getInstance()
                .retrieve(event.getGuild().getIdLong())
                .getChildChannelIds()
                .removeIf(childChannelId -> childChannelId == null || childChannelId == event.getChannel().getIdLong());

        return true;
    }

}
