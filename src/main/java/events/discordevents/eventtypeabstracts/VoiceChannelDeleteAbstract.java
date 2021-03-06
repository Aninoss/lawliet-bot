package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import java.util.ArrayList;

public abstract class VoiceChannelDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onVoiceChannelDelete(VoiceChannelDeleteEvent event) throws Throwable;

    public static void onVoiceChannelDeleteStatic(VoiceChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((VoiceChannelDeleteAbstract) listener).onVoiceChannelDelete(event)
        );
    }

}
