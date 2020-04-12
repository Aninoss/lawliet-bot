package DiscordListener;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.server.ServerJoinEvent;

import java.util.concurrent.ExecutionException;

public class ServerJoinListener {

    public void onServerJoin(ServerJoinEvent event) throws Exception {
        DBServer.getInstance().getBean(event.getServer().getId());

        DiscordApiCollection.getInstance().getRandomWritableChannel(event.getServer()).ifPresent(this::sendNewMessage);
        DiscordApiCollection.getInstance().insertWebhook(event.getServer());
        if (event.getServer().getMembers().size() >= 500)
            DiscordApiCollection.getInstance().getOwner().sendMessage("**+++** " + event.getServer().getName() + " (" + event.getServer().getMembers().size() + ")");
    }

    private void sendNewMessage(ServerTextChannel channel) {
        String text = "Hi! Thanks for inviting me to your lovely server! <3\n\nJust write `L.help` to get an overview of all my commands.\nIf these commands should only work in certain channels, run `L.whitelist`\nWith `L.fishery` you can configure the tactical \"level system\" idle game and read how it works.\nFurthermore, if you wanna switch the language to German, write `L.language de`\n\nAnd finally, if you have any issues with the bot, try `L.faq`. You can also just join the Lawliet Support server and ask for help:\n\n\uD83D\uDC49\uD83D\uDC49 [Lawliet Support Server](" + Settings.SERVER_INVITE_URL + ") \uD83D\uDC48\uD83D\uDC48";
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setAuthor(DiscordApiCollection.getInstance().getYourself())
                .setThumbnail(DiscordApiCollection.getInstance().getYourself().getAvatar())
                .setDescription(text);
        try {
            channel.sendMessage(eb).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

}
