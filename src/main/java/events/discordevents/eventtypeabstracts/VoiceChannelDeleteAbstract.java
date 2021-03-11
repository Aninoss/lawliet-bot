package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;

public abstract class VoiceChannelDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onVoiceChannelDelete(VoiceChannelDeleteEvent event) throws Throwable;

    public static void onVoiceChannelDeleteStatic(VoiceChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((VoiceChannelDeleteAbstract) listener).onVoiceChannelDelete(event)
        );
    }

}
