package events.discordevents.voicechannelcreate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelCreateAbstract;
import modules.Mute;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent;

@DiscordEvent
public class VoiceChannelCreateEnforceMuteRole extends VoiceChannelCreateAbstract {

    @Override
    public boolean onVoiceChannelCreate(VoiceChannelCreateEvent event) {
        Mute.enforceMuteRole(event.getGuild());
        return true;
    }

}