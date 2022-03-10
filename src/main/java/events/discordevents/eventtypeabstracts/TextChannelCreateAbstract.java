package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;

public abstract class TextChannelCreateAbstract extends DiscordEventAbstract {

    public abstract boolean onTextChannelCreate(ChannelCreateEvent event) throws Throwable;

    public static void onTextChannelCreateStatic(ChannelCreateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((TextChannelCreateAbstract) listener).onTextChannelCreate(event)
        );
    }

}
