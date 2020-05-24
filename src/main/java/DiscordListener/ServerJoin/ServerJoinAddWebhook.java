package DiscordListener.ServerJoin;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.ServerJoinAbstract;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.server.ServerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

@DiscordListenerAnnotation
public class ServerJoinAddWebhook extends ServerJoinAbstract {

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DiscordApiCollection.getInstance().insertWebhook(event.getServer());
        return true;
    }

}
