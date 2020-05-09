package DiscordListener;

import Constants.Settings;
import Core.PatreonCache;
import Modules.BannedWordsCheck;
import org.javacord.api.event.server.role.UserRoleAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRoleAddListener {

    final static Logger LOGGER = LoggerFactory.getLogger(UserRoleAddListener.class);

    public void onUserRoleAdd(UserRoleAddEvent event) {
        if (event.getUser().isBot()) return;

        if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.DONATION_ROLE_IDS) {
                if (event.getRole().getId() == roleId) {
                    LOGGER.info("NEW PATREON {} ({})", event.getUser().getName(), event.getUser().getId());
                    PatreonCache.getInstance().resetUser(event.getUser().getId());
                    break;
                }
            }
        }
    }

}
