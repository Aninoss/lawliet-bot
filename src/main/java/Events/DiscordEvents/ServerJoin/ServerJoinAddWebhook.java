package Events.DiscordEvents.ServerJoin;

import Core.DiscordApiCollection;
import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventTypeAbstracts.ServerJoinAbstract;
import org.javacord.api.event.server.ServerJoinEvent;

@DiscordEvent
public class ServerJoinAddWebhook extends ServerJoinAbstract {

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DiscordApiCollection.getInstance().insertWebhook(event.getServer());
        return true;
    }

}
