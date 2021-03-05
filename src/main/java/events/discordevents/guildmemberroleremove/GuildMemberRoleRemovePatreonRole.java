package events.discordevents.guildmemberroleremove;

import constants.AssetIds;
import constants.Settings;
import core.ShardManager;
import core.MainLogger;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleRemoveAbstract;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@DiscordEvent
public class GuildMemberRoleRemovePatreonRole extends GuildMemberRoleRemoveAbstract {

    @Override
    public boolean onUserRoleRemove(UserRoleRemoveEvent event) throws Throwable {
        if (event.getServer().getId() == AssetIds.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getRole().getId() == roleId) {
                    MainLogger.get().info("PATREON LEFT {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                    ShardManager.getInstance().fetchOwner().get().sendMessage("PATREON USER LEFT: " + StringUtil.escapeMarkdown(event.getUser().getDiscriminatedName())).exceptionally(ExceptionLogger.get());
                    break;
                }
            }
        }

        return true;
    }

}
