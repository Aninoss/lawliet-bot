package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import core.cache.MessageCache;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;

public abstract class GuildMessageUpdateAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageUpdate(MessageUpdateEvent event) throws Throwable;

    public static void onGuildMessageUpdateStatic(MessageUpdateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        MessageCache.update(event.getMessage());

        Member member = event.getMember();
        if (member == null) {
            return;
        }

        execute(listenerList, member.getUser(), event.getGuild().getIdLong(),
                listener -> ((GuildMessageUpdateAbstract) listener).onGuildMessageUpdate(event)
        );
    }

}
