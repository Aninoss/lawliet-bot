package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent;

public abstract class GuildUpdateBoostCountAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildUpdateBoostCount(GuildUpdateBoostCountEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildUpdateBoostCountStatic(GuildUpdateBoostCountEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                (listener, entityManager) -> ((GuildUpdateBoostCountAbstract) listener).onGuildUpdateBoostCount(event, entityManager)
        );
    }

}
