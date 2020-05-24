package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class UserRoleRemoveAbstract extends DiscordListenerAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(UserRoleRemoveAbstract.class);

    public abstract boolean onUserRoleRemove(UserRoleRemoveEvent event) throws Throwable;

    public static void onUserRoleRemoveStatic(UserRoleRemoveEvent event, ArrayList<DiscordListenerAbstract> listenerList) {
        if (event.getUser().isBot()) return;

        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof UserRoleRemoveAbstract) {
                UserRoleRemoveAbstract userRoleRemoveAbstract = (UserRoleRemoveAbstract) listener;

                try {
                    if (!userRoleRemoveAbstract.onUserRoleRemove(event)) return;
                } catch (InterruptedException interrupted) {
                    LOGGER.error("Interrupted", interrupted);
                    return;
                } catch (Throwable throwable) {
                    LOGGER.error("Uncaught exception", throwable);
                }
            }
        }
    }

}
