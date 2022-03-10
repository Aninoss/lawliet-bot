package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateUserLimitEvent;

public abstract class VoiceChannelUpdateUserLimitAbstract extends DiscordEventAbstract {

    public abstract boolean onVoiceChannelUpdateUserLimit(ChannelUpdateUserLimitEvent event) throws Throwable;

    public static void onVoiceChannelUpdateUserLimitStatic(ChannelUpdateUserLimitEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((VoiceChannelUpdateUserLimitAbstract) listener).onVoiceChannelUpdateUserLimit(event)
        );
    }

}
