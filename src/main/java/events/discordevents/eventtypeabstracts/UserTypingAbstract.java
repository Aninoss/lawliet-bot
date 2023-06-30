package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.user.UserTypingEvent;

public abstract class UserTypingAbstract extends DiscordEventAbstract {

    public abstract boolean onUserTyping(UserTypingEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onUserTypingStatic(UserTypingEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getGuild() != null) {
            execute(listenerList, event.getUser(), event.getGuild().getIdLong(),
                    (listener, entityManager) -> ((UserTypingAbstract) listener).onUserTyping(event, entityManager)
            );
        }
    }

}
