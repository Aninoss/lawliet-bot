package Events.DiscordEvents.ServerLeave;

import CommandSupporters.CommandLogger.CommandLogger;
import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventTypeAbstracts.ServerLeaveAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;

@DiscordEvent
public class ServerLeaveSaveLog extends ServerLeaveAbstract {

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 1000) {
            CommandLogger.getInstance().saveLog(event.getServer().getId(), true);
        }

        return true;
    }

}
