package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.command.ApplicationUpdatePrivilegesEvent;

public abstract class ApplicationUpdatePrivilegesAbstract extends DiscordEventAbstract {

    public abstract boolean onApplicationUpdatePrivileges(ApplicationUpdatePrivilegesEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onApplicationUpdatePrivilegesStatic(ApplicationUpdatePrivilegesEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                (listener, entityManager) -> ((ApplicationUpdatePrivilegesAbstract) listener).onApplicationUpdatePrivileges(event, entityManager)
        );
    }

}
