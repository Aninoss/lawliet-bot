package DiscordEvents.ServerMemberJoin;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.PatreonCache;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberJoinAbstract;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class ServerMemberJoinPatreon extends ServerMemberJoinAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinPatreon.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID) {
            for(long roleId : Settings.DONATION_ROLE_IDS) {
                if (event.getServer().getRoles(event.getUser()).stream().anyMatch(role -> role.getId() == roleId)) {
                    LOGGER.info("NEW PATREON {} ({})", event.getUser().getDiscriminatedName(), event.getUser().getId());
                    DiscordApiCollection.getInstance().getOwner().sendMessage("NEW PATREON USER: " + event.getUser().getDiscriminatedName());
                    PatreonCache.getInstance().resetUser(event.getUser().getId());
                    break;
                }
            }
        }

        return true;
    }

}
