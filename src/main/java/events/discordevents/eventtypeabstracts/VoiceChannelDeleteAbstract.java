package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

public abstract class VoiceChannelDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onVoiceChannelDelete(ChannelDeleteEvent event) throws Throwable;

    public static void onVoiceChannelDeleteStatic(ChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((VoiceChannelDeleteAbstract) listener).onVoiceChannelDelete(event)
        );
    }

}
