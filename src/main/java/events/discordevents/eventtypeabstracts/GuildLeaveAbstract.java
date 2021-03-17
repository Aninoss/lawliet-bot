package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;

public abstract class GuildLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildLeave(GuildLeaveEvent event) throws Throwable;

    public static void onGuildLeaveStatic(GuildLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, 0L,
                listener -> ((GuildLeaveAbstract) listener).onGuildLeave(event)
        );
    }

}
