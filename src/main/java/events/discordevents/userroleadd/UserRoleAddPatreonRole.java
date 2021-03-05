package events.discordevents.userroleadd;

import constants.AssetIds;
import constants.Settings;
import core.DiscordApiManager;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.UserRoleAddAbstract;
import org.javacord.api.event.server.role.UserRoleAddEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent
public class UserRoleAddPatreonRole extends UserRoleAddAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserRoleAddPatreonRole.class);

    @Override
    public boolean onUserRoleAdd(UserRoleAddEvent event) throws Throwable {
        if (event.getServer().getId() == AssetIds.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getRole().getId() == roleId) {
                    MainLogger.get().info("NEW PATREON {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                    DiscordApiManager.getInstance().fetchOwner().get().sendMessage("NEW PATREON USER: " + StringUtil.escapeMarkdown(event.getUser().getDiscriminatedName())).exceptionally(ExceptionLogger.get());
                    PatreonCache.getInstance().requestUpdate();
                    break;
                }
            }
        }

        return true;
    }

}
