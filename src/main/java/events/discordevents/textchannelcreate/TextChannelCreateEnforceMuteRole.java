package events.discordevents.textchannelcreate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.TextChannelCreateAbstract;
import modules.Mute;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;

@DiscordEvent
public class TextChannelCreateEnforceMuteRole extends TextChannelCreateAbstract {

    @Override
    public boolean onTextChannelCreate(TextChannelCreateEvent event) {
        Mute.enforceMuteRole(event.getGuild());
        return true;
    }

}