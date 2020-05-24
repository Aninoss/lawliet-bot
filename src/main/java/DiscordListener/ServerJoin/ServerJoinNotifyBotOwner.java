package DiscordListener.ServerJoin;

import Core.DiscordApiCollection;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.ServerJoinAbstract;
import org.javacord.api.event.server.ServerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordListenerAnnotation
public class ServerJoinNotifyBotOwner extends ServerJoinAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerJoinNotifyBotOwner.class);

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 500)
            DiscordApiCollection.getInstance().getOwner().sendMessage("**+++** " + event.getServer().getName() + " (" + event.getServer().getMemberCount() + ")");

        LOGGER.info("+++ {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
        return true;
    }

}
