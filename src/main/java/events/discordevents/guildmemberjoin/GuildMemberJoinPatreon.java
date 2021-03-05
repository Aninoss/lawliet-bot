package events.discordevents.guildmemberjoin;

import constants.AssetIds;
import constants.Settings;
import core.ShardManager;
import core.MainLogger;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@DiscordEvent
public class GuildMemberJoinPatreon extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        if (event.getServer().getId() == AssetIds.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getServer().getRoles(event.getUser()).stream().anyMatch(role -> role.getId() == roleId)) {
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
