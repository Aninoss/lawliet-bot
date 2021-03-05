package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent;
import java.util.ArrayList;

public abstract class GuildUpdateBoostCountAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildUpdateBoostCount(GuildUpdateBoostCountEvent event) throws Throwable;

    public static void onGuildUpdateBoostCountStatic(GuildUpdateBoostCountEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GuildUpdateBoostCountAbstract) listener).onGuildUpdateBoostCount(event)
        );
    }

}
