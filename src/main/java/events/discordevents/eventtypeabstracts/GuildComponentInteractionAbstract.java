package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import core.buttons.GuildComponentInteractionEvent;
import events.discordevents.DiscordEventAbstract;

public abstract class GuildComponentInteractionAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildComponentInteraction(GuildComponentInteractionEvent event) throws Throwable;

    public static void onGuildComponentInteractionStatic(GuildComponentInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getMember().getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildComponentInteractionAbstract) listener).onGuildComponentInteraction(event)
        );
    }

}
