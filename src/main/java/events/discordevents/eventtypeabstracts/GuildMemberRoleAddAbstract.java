package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;

public abstract class GuildMemberRoleAddAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMemberRoleAdd(GuildMemberRoleAddEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildMemberRoleAddStatic(GuildMemberRoleAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildMemberRoleAddAbstract) listener).onGuildMemberRoleAdd(event, entityManager)
        );
    }

}
