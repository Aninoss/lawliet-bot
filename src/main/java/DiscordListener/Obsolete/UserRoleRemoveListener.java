package DiscordListener.Obsolete;

import Constants.Settings;
import Core.PatreonCache;
import org.javacord.api.event.server.role.UserRoleAddEvent;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;

public class UserRoleRemoveListener {

    public void onUserRoleRemove(UserRoleRemoveEvent event) {
        if (event.getUser().isYourself()) return;

        if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.DONATION_ROLE_IDS) {
                if (event.getRole().getId() == roleId) {
                    PatreonCache.getInstance().resetUser(event.getUser().getId());
                    break;
                }
            }
        }
    }

}
