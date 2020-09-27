package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.server.role.UserRoleAddEvent;

import java.util.ArrayList;

public abstract class UserRoleAddAbstract extends DiscordEventAbstract {

    public abstract boolean onUserRoleAdd(UserRoleAddEvent event) throws Throwable;

    public static void onUserRoleAddStatic(UserRoleAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), true,
                listener -> ((UserRoleAddAbstract) listener).onUserRoleAdd(event)
        );
    }

}
