package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;

public abstract class GuildMemberRoleRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildMemberRoleRemoveStatic(GuildMemberRoleRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildMemberRoleRemoveAbstract) listener).onGuildMemberRoleRemove(event, entityManager)
        );
    }

}
