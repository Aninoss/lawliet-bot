package Events.DiscordEvents.ServerJoin;

import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventPriority;
import Events.DiscordEvents.EventTypeAbstracts.ServerJoinAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.ServerJoinEvent;

@DiscordEvent(priority = EventPriority.HIGH)
public class ServerJoinRegister extends ServerJoinAbstract {

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DBServer.getInstance().getBean(event.getServer().getId());
        return true;
    }

}
