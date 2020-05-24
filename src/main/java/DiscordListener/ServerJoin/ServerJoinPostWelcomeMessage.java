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
public class ServerJoinPostWelcomeMessage extends ServerJoinAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerJoinPostWelcomeMessage.class);

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DiscordApiCollection.getInstance().getFirstWritableChannel(event.getServer()).ifPresent(this::sendNewMessage);
        return true;
    }

    private void sendNewMessage(ServerTextChannel channel) {
        String text = "Hi! Thanks for inviting me to your lovely server! <3\n\nJust write `L.help` to get an overview of all my commands.\nIf these commands should only work in certain channels, run `L.whitelist`\nWith `L.fishery` you can configure the tactical \"level system\" idle game and read how it works.\nFurthermore, if you wanna switch the language to German, write `L.language de`\n\nAnd finally, if you have any issues with the bot, try `L.faq`. You can also just join the Lawliet Support server and ask for help:\n\n\uD83D\uDC49\uD83D\uDC49 [Lawliet Support Server](" + Settings.SERVER_INVITE_URL + ") \uD83D\uDC48\uD83D\uDC48";
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setAuthor(DiscordApiCollection.getInstance().getYourself())
                .setThumbnail(DiscordApiCollection.getInstance().getYourself().getAvatar())
                .setDescription(text);
        try {
            if (channel.canYouWrite() && channel.canYouEmbedLinks()) channel.sendMessage(eb).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not send server join message", e);
        }
    }

}
