package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;

public abstract class GuildMemberUpdatePendingAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildMemberUpdatePendingStatic(GuildMemberUpdatePendingEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildMemberUpdatePendingAbstract) listener).onGuildMemberUpdatePending(event, entityManager)
        );
    }

}
