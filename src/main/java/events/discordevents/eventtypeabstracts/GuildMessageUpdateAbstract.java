package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import core.cache.MessageCache;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;

public abstract class GuildMessageUpdateAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMessageUpdate(MessageUpdateEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildMessageUpdateStatic(MessageUpdateEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        MessageCache.update(event.getMessage());
        if (event.getMessage().getType().isSystem()) {
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            return;
        }

        execute(listenerList, member.getUser(), event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildMessageUpdateAbstract) listener).onGuildMessageUpdate(event, entityManager)
        );
    }

}
