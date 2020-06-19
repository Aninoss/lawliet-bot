package DiscordEvents.ServerMemberLeave;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.PatreonCache;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberLeaveAbstract;
import DiscordEvents.EventTypeAbstracts.UserRoleRemoveAbstract;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.javacord.api.event.server.role.UserRoleRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class ServerMemberLeavePatreon extends ServerMemberLeaveAbstract {

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        if (event.getServer().getId() == Settings.SUPPORT_SERVER_ID)
            PatreonCache.getInstance().resetUser(event.getUser().getId());

        return true;
    }

}
