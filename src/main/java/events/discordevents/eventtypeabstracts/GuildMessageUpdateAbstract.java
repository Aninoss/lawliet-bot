package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;

public abstract class GuildMessageUpdateAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageUpdate(GuildMessageUpdateEvent event) throws Throwable;

    public static void onGuildMessageUpdateStatic(GuildMessageUpdateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        Member member = event.getMember();
        if (member == null)
            return;

        execute(listenerList, member.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMessageUpdateAbstract) listener).onGuildMessageUpdate(event)
        );
    }

}
