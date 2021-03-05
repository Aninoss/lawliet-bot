package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import java.util.ArrayList;

public abstract class GuildMessageReactionAddAbstract extends DiscordEventAbstract {

    public abstract boolean onReactionAdd(GuildMessageReactionAddEvent event) throws Throwable;

    public static void onGuildMessageReactionAddStatic(GuildMessageReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMessageReactionAddAbstract) listener).onReactionAdd(event)
        );
    }

}
