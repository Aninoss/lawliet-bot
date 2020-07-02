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

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberLeavePatreon.class);

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID) {
            PatreonCache.getInstance().resetUser(event.getUser().getId());
            DiscordApiCollection.getInstance().getOwner().sendMessage("User left roles: " + event.getServer().getRoles(event.getUser()).size());

            for(long roleId : Settings.DONATION_ROLE_IDS) {
                if (event.getServer().getRoles(event.getUser()).stream().anyMatch(role -> role.getId() == roleId)) {
                    LOGGER.info("PATREON LEFT {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                    DiscordApiCollection.getInstance().getOwner().sendMessage("PATREON USER LEFT: " + event.getUser().getDiscriminatedName());
                    //PatreonCache.getInstance().resetUser(event.getUser().getId());
                    break;
                }
            }
        }

        return true;
    }

}
