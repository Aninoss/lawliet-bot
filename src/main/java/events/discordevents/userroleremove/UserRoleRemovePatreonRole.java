package events.discordevents.userroleremove;

import constants.AssetIds;
import constants.ExternalLinks;
import constants.Settings;
import core.DiscordApiCollection;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.UserRoleRemoveAbstract;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent
public class UserRoleRemovePatreonRole extends UserRoleRemoveAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserRoleRemovePatreonRole.class);

    @Override
    public boolean onUserRoleRemove(UserRoleRemoveEvent event) throws Throwable {
        if (event.getServer().getId() == AssetIds.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getRole().getId() == roleId) {
                    PatreonCache.getInstance().resetUser(event.getUser().getId());
                    if (PatreonCache.getInstance().getPatreonLevel(event.getUser().getId()) == 0) {
                        LOGGER.info("PATREON LEFT {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                        event.getUser().sendMessage(String.format("**❌ You are no longer registered as a Patreon!**\nIf this was unexpected, please get in touch with us in the Lawliet server: %s",
                                ExternalLinks.SERVER_INVITE_URL
                        ));
                        DiscordApiCollection.getInstance().getOwner().sendMessage("PATREON USER LEFT: " + StringUtil.escapeMarkdown(event.getUser().getDiscriminatedName())).exceptionally(ExceptionLogger.get());
                    }
                    break;
                }
            }
        }

        return true;
    }

}
