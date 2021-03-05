package events.discordevents.guildmemberremove;

import constants.AssetIds;
import core.ShardManager;
import core.MainLogger;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@DiscordEvent
public class GuildMemberRemovePatreon extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(ServerMemberLeaveEvent event) throws Throwable {
        if (event.getServer().getId() == AssetIds.SUPPORT_SERVER_ID) {
            if (PatreonCache.getInstance().getUserTier(event.getUser().getId()) > 0) {
                MainLogger.get().info("PATREON LEFT (LEFT SERVER) {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                ShardManager.getInstance().fetchOwner().get().sendMessage("PATREON USER LEFT (LEFT SERVER): " + StringUtil.escapeMarkdown(event.getUser().getDiscriminatedName())).exceptionally(ExceptionLogger.get());
            }
        }

        return true;
    }

}
