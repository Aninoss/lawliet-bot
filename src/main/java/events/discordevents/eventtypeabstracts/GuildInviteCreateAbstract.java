package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;

public abstract class GuildInviteCreateAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildInviteCreate(GuildInviteCreateEvent event) throws Throwable;

    public static void onGuildInviteCreateStatic(GuildInviteCreateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                listener -> ((GuildInviteCreateAbstract) listener).onGuildInviteCreate(event)
        );
    }

}
