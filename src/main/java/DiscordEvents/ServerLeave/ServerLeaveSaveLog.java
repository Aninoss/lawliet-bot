package DiscordEvents.ServerLeave;

import CommandSupporters.CommandLogger.CommandLogger;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerLeaveAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;

@DiscordEventAnnotation
public class ServerLeaveSaveLog extends ServerLeaveAbstract {

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 100) {
            CommandLogger.getInstance().saveLog(event.getServer().getId(), true);
        }

        return true;
    }

}
