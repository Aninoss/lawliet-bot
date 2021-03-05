package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import java.util.ArrayList;

public abstract class GuildMessageReactionRemoveAbstract extends DiscordEventAbstract {
    
    public abstract boolean onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) throws Throwable;

    public static void onGuildMessageReactionRemoveStatic(GuildMessageReactionRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMessageReactionRemoveAbstract) listener).onGuildMessageReactionRemove(event)
        );
    }

}
