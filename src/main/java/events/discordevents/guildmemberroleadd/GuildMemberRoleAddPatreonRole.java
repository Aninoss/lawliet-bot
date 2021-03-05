package events.discordevents.guildmemberroleadd;

import constants.AssetIds;
import constants.Settings;
import core.ShardManager;
import core.MainLogger;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleAddAbstract;
import org.javacord.api.event.server.role.UserRoleAddEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@DiscordEvent
public class GuildMemberRoleAddPatreonRole extends GuildMemberRoleAddAbstract {

    @Override
    public boolean onGuildMemberRoleAdd(UserRoleAddEvent event) throws Throwable {
        if (event.getServer().getId() == AssetIds.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getRole().getId() == roleId) {
                    MainLogger.get().info("NEW PATREON {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                    ShardManager.getInstance().fetchOwner().get().sendMessage("NEW PATREON USER: " + StringUtil.escapeMarkdown(event.getUser().getDiscriminatedName())).exceptionally(ExceptionLogger.get());
                    PatreonCache.getInstance().requestUpdate();
                    break;
                }
            }
        }

        return true;
    }

}
