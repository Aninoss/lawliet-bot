package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.command.ApplicationCommandUpdatePrivilegesEvent;

public abstract class ApplicationCommandUpdatePrivilegesAbstract extends DiscordEventAbstract {

    public abstract boolean onApplicationCommandUpdatePrivileges(ApplicationCommandUpdatePrivilegesEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onApplicationCommandUpdatePrivilegesStatic(ApplicationCommandUpdatePrivilegesEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getGuild().getIdLong(),
                (listener, entityManager) -> ((ApplicationCommandUpdatePrivilegesAbstract) listener).onApplicationCommandUpdatePrivileges(event, entityManager)
        );
    }

}
