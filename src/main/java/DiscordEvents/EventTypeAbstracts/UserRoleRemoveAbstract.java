package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class UserRoleRemoveAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(UserRoleRemoveAbstract.class);

    public abstract boolean onUserRoleRemove(UserRoleRemoveEvent event) throws Throwable;

    public static void onUserRoleRemoveStatic(UserRoleRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isBot()) return;

        boolean banned = userIsBanned(event.getUser().getId());

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof UserRoleRemoveAbstract) {
                UserRoleRemoveAbstract userRoleRemoveAbstract = (UserRoleRemoveAbstract) listener;
                if (banned && !userRoleRemoveAbstract.isAllowingBannedUser()) continue;

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
