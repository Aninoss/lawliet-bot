package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;

public abstract class GuildMessageDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageDelete(MessageDeleteEvent event) throws Throwable;

    public static void onGuildMessageDeleteStatic(MessageDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GuildMessageDeleteAbstract) listener).onGuildMessageDelete(event)
        );
    }

}
