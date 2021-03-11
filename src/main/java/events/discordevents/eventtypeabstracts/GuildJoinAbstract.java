package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

public abstract class GuildJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildJoin(GuildJoinEvent event) throws Throwable;

    public static void onGuildJoinStatic(GuildJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GuildJoinAbstract) listener).onGuildJoin(event)
        );
    }

}
