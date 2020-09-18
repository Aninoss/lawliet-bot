package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class UserRoleRemoveAbstract extends DiscordEventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserRoleRemoveAbstract.class);

    public abstract boolean onUserRoleRemove(UserRoleRemoveEvent event) throws Throwable;

    public static void onUserRoleRemoveStatic(UserRoleRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((UserRoleRemoveAbstract) listener).onUserRoleRemove(event)
        );
    }

}
