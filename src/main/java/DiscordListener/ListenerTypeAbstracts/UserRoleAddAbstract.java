package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.event.server.role.UserRoleAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class UserRoleAddAbstract extends DiscordListenerAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(UserRoleAddAbstract.class);

    public abstract boolean onUserRoleAdd(UserRoleAddEvent event) throws Throwable;

    public static void onUserRoleAddStatic(UserRoleAddEvent event, ArrayList<DiscordListenerAbstract> listenerList) {
        if (event.getUser().isBot()) return;

        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof UserRoleAddAbstract) {
                UserRoleAddAbstract userRoleAddAbstract = (UserRoleAddAbstract) listener;

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
