package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;

public abstract class GuildInviteDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildInviteDelete(GuildInviteDeleteEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildInviteDeleteStatic(GuildInviteDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildInviteDeleteAbstract) listener).onGuildInviteDelete(event, entityManager)
        );
    }

}
