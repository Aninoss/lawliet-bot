package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;

public abstract class GuildInviteDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildInviteDelete(GuildInviteDeleteEvent event) throws Throwable;

    public static void onGuildInviteDeleteStatic(GuildInviteDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GuildInviteDeleteAbstract) listener).onGuildInviteDelete(event)
        );
    }

}
