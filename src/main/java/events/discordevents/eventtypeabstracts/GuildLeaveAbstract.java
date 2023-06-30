package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;

public abstract class GuildLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onGuildLeave(GuildLeaveEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onGuildLeaveStatic(GuildLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, 0L,
                (listener, entityManager) -> ((GuildLeaveAbstract) listener).onGuildLeave(event, entityManager)
        );
    }

}
