package DiscordEvents.ServerMemberLeave;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.PatreonCache;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberLeaveAbstract;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class ServerMemberLeavePatreon extends ServerMemberLeaveAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberLeavePatreon.class);

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID) {
            if (PatreonCache.getInstance().getPatreonLevel(event.getUser().getId()) > 0) {
                LOGGER.info("PATREON LEFT (LEFT SERVER) {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                DiscordApiCollection.getInstance().getOwner().sendMessage("PATREON USER LEFT (LEFT SERVER): " + event.getUser().getDiscriminatedName());
            }
            PatreonCache.getInstance().resetUser(event.getUser().getId());
        }

        return true;
    }

}
