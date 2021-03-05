package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import java.util.ArrayList;

public abstract class GuildLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildLeave(GuildLeaveEvent event) throws Throwable;

    public static void onGuildLeaveStatic(GuildLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GuildLeaveAbstract) listener).onGuildLeave(event)
        );
    }

}
