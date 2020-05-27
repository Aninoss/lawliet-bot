package DiscordEvents.ServerJoin;

import Core.DiscordApiCollection;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerJoinAbstract;
import org.javacord.api.event.server.ServerJoinEvent;

@DiscordEventAnnotation
public class ServerJoinAddWebhook extends ServerJoinAbstract {

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DiscordApiCollection.getInstance().insertWebhook(event.getServer());
        return true;
    }

}
