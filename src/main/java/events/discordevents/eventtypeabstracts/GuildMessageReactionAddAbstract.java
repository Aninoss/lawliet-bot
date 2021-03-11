package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

public abstract class GuildMessageReactionAddAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) throws Throwable;

    public static void onGuildMessageReactionAddStatic(GuildMessageReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMessageReactionAddAbstract) listener).onGuildMessageReactionAdd(event)
        );
    }

}
