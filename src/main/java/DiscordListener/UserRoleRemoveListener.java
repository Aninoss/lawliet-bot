package DiscordListener;

import Constants.Settings;
import Core.Utils.BotUtil;
import MySQL.Modules.PatreonServerUnlock.DBPatreonServerUnlock;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRoleRemoveListener {

    final static Logger LOGGER = LoggerFactory.getLogger(UserRoleRemoveListener.class);

    public void onUserRoleRemove(UserRoleRemoveEvent event) {
        if (event.getUser().isYourself()) return;

        try {
            if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID) {
                for (long roleId : Settings.DONATION_ROLE_IDS) {
                    if (roleId == event.getRole().getId() && BotUtil.getUserDonationStatus(event.getUser()) == 0) {
                        DBPatreonServerUnlock.getInstance().removeUser(event.getUser().getId());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception in donation remove", e);
        }
    }

}