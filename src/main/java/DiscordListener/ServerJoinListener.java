package DiscordListener;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.server.ServerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class ServerJoinListener {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerJoinListener.class);

    public void onServerJoin(ServerJoinEvent event) throws Exception {
        DBServer.getInstance().getBean(event.getServer().getId());

        DiscordApiCollection.getInstance().getRandomWritableChannel(event.getServer()).ifPresent(this::sendNewMessage);
        DiscordApiCollection.getInstance().insertWebhook(event.getServer());
        if (event.getServer().getMemberCount() >= 500)
            DiscordApiCollection.getInstance().getOwner().sendMessage("**+++** " + event.getServer().getName() + " (" + event.getServer().getMemberCount() + ")");

        LOGGER.info("+++ {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
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
