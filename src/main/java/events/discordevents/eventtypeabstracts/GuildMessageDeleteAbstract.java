package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;

public abstract class GuildMessageDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageDelete(GuildMessageDeleteEvent event) throws Throwable;

    public static void onGuildMessageDeleteStatic(GuildMessageDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GuildMessageDeleteAbstract) listener).onGuildMessageDelete(event)
        );
    }

}
