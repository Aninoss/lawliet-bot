package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;

public abstract class GuildUnbanAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildUnban(GuildUnbanEvent event) throws Throwable;

    public static void onGuildUnbanStatic(GuildUnbanEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildUnbanAbstract) listener).onGuildUnban(event)
        );
    }

}
