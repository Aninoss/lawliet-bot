package events.discordevents.servermemberjoin;

import constants.AssetIds;
import constants.Settings;
import core.DiscordApiManager;
import core.cache.PatreonCache;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerMemberJoinAbstract;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent
public class ServerMemberJoinPatreon extends ServerMemberJoinAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinPatreon.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        if (event.getServer().getId() == AssetIds.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getServer().getRoles(event.getUser()).stream().anyMatch(role -> role.getId() == roleId)) {
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
