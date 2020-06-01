package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.role.UserRoleAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class UserRoleAddAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(UserRoleAddAbstract.class);

    public abstract boolean onUserRoleAdd(UserRoleAddEvent event) throws Throwable;

    public static void onUserRoleAddStatic(UserRoleAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isBot()) return;

        boolean banned = userIsBanned(event.getUser().getId());

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof UserRoleAddAbstract) {
                UserRoleAddAbstract userRoleAddAbstract = (UserRoleAddAbstract) listener;
                if (banned && !userRoleAddAbstract.isAllowingBannedUser()) continue;

                try {
                    if (!userRoleAddAbstract.onUserRoleAdd(event)) return;
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
