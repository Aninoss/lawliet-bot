package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;

public abstract class GuildMessageContextInteractionAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageContextInteraction(MessageContextInteractionEvent event) throws Throwable;

    public static void onGuildMessageContextInteractionStatic(MessageContextInteractionEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMessageContextInteractionAbstract) listener).onGuildMessageContextInteraction(event)
        );
    }

}
