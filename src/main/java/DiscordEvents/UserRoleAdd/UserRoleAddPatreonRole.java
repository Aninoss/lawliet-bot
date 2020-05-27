package DiscordEvents.UserRoleAdd;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.PatreonCache;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.UserRoleAddAbstract;
import org.javacord.api.event.server.role.UserRoleAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class UserRoleAddPatreonRole extends UserRoleAddAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(UserRoleAddPatreonRole.class);

    @Override
    public boolean onUserRoleAdd(UserRoleAddEvent event) throws Throwable {
        if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.DONATION_ROLE_IDS) {
                if (event.getRole().getId() == roleId) {
                    LOGGER.info("NEW PATREON {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                    DiscordApiCollection.getInstance().getOwner().sendMessage("NEW PATREON USER: " + event.getUser().getDiscriminatedName());
                    PatreonCache.getInstance().resetUser(event.getUser().getId());
                    break;
                }
            }
        }

        return true;
    }

}
