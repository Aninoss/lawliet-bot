package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;

public abstract class VoiceChannelUpdateUserLimitAbstract extends DiscordEventAbstract {

    public abstract boolean onVoiceChannelUpdateUserLimit(VoiceChannelUpdateUserLimitEvent event) throws Throwable;

    public static void onVoiceChannelUpdateUserLimitStatic(VoiceChannelUpdateUserLimitEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((VoiceChannelUpdateUserLimitAbstract) listener).onVoiceChannelUpdateUserLimit(event)
        );
    }

}
