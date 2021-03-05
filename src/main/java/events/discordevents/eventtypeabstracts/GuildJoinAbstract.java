package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

import java.util.ArrayList;

public abstract class GuildJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildJoin(GuildJoinEvent event) throws Throwable;

    public static void onGuildJoinStatic(GuildJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GuildJoinAbstract) listener).onGuildJoin(event)
        );
    }

}
