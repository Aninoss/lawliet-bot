package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;

import java.util.ArrayList;

public abstract class UserRoleRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onUserRoleRemove(UserRoleRemoveEvent event) throws Throwable;

    public static void onUserRoleRemoveStatic(UserRoleRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), true,
                listener -> ((UserRoleRemoveAbstract) listener).onUserRoleRemove(event)
        );
    }

}
