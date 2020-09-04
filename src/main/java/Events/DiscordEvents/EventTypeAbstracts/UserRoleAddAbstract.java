package Events.DiscordEvents.EventTypeAbstracts;

import Events.DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.role.UserRoleAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class UserRoleAddAbstract extends DiscordEventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserRoleAddAbstract.class);

    public abstract boolean onUserRoleAdd(UserRoleAddEvent event) throws Throwable;

    public static void onUserRoleAddStatic(UserRoleAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((UserRoleAddAbstract) listener).onUserRoleAdd(event)
        );
    }

}
