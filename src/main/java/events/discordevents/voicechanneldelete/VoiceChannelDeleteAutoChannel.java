package events.discordevents.voicechanneldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelDeleteAbstract;
import mysql.modules.autochannel.DBAutoChannel;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;

@DiscordEvent
public class VoiceChannelDeleteAutoChannel extends VoiceChannelDeleteAbstract {

    @Override
    public boolean onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
        DBAutoChannel.getInstance()
                .retrieve(event.getGuild().getIdLong())
                .getChildChannelIds()
                .removeIf(childChannelId -> childChannelId == null || childChannelId == event.getChannel().getIdLong());

        return true;
    }

}
