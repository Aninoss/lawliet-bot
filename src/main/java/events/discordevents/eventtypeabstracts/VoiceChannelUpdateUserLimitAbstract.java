package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;
import java.util.ArrayList;

public abstract class VoiceChannelUpdateUserLimitAbstract extends DiscordEventAbstract {

    public abstract boolean onVoiceChannelUpdateUserLimit(VoiceChannelUpdateUserLimitEvent event) throws Throwable;

    public static void onVoiceChannelUpdateUserLimitStatic(VoiceChannelUpdateUserLimitEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((VoiceChannelUpdateUserLimitAbstract) listener).onVoiceChannelUpdateUserLimit(event)
        );
    }

}
