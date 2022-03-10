package events.discordevents.voicechannelcreate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelCreateAbstract;
import modules.Mute;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;

@DiscordEvent
public class VoiceChannelCreateEnforceMuteRole extends VoiceChannelCreateAbstract {

    @Override
    public boolean onVoiceChannelCreate(ChannelCreateEvent event) {
        Mute.enforceMuteRole(event.getGuild());
        return true;
    }

}