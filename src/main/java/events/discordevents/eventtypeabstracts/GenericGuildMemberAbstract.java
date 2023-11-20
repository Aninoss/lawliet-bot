package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;

import java.util.ArrayList;

public abstract class GenericGuildMemberAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMember(GenericGuildMemberEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildMemberStatic(GenericGuildMemberEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GenericGuildMemberAbstract) listener).onGuildMember(event, entityManager)
        );
    }

}
