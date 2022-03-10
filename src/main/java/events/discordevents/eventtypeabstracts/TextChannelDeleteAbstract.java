package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

public abstract class TextChannelDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onTextChannelDelete(ChannelDeleteEvent event) throws Throwable;

    public static void onTextChannelDeleteStatic(ChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((TextChannelDeleteAbstract) listener).onTextChannelDelete(event)
        );
    }

}
