package DiscordEvents.UserRoleRemove;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.PatreonCache;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.UserRoleRemoveAbstract;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class UserRoleRemovePatreonRole extends UserRoleRemoveAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserRoleRemovePatreonRole.class);

    @Override
    public boolean onUserRoleRemove(UserRoleRemoveEvent event) throws Throwable {
        if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.DONATION_ROLE_IDS) {
                if (event.getRole().getId() == roleId) {
                    LOGGER.info("PATREON LEFT {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                    DiscordApiCollection.getInstance().getOwner().sendMessage("PATREON USER LEFT: " + event.getUser().getDiscriminatedName());
                    PatreonCache.getInstance().resetUser(event.getUser().getId());
                    break;
                }
            }
        }

        return true;
    }

}
