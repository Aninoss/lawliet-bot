package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

public abstract class GuildMemberRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMemberRemove(GuildMemberRemoveEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildMemberRemoveStatic(GuildMemberRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildMemberRemoveAbstract) listener).onGuildMemberRemove(event, entityManager)
        );
    }

}
