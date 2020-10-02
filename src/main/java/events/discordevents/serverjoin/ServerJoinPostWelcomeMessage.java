package events.discordevents.serverjoin;

import constants.ExternalLinks;
import core.DiscordApiCollection;
import core.EmbedFactory;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerJoinAbstract;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent
public class ServerJoinPostWelcomeMessage extends ServerJoinAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerJoinPostWelcomeMessage.class);

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DiscordApiCollection.getInstance().getFirstWritableChannel(event.getServer()).ifPresent(this::sendNewMessage);
        return true;
    }

    private void sendNewMessage(ServerTextChannel channel) {
        String text = "Hi! Thanks for inviting me to your lovely server! <3\n\nJust write `L.help` to get an overview of all my commands.\nIf these commands should only work in certain channels, run `L.whitelist`\nWith `L.fishery` you can configure the tactical \"level system\" idle game and read how it works.\nFurthermore, you can also change the bot language: German - `L.language de`; Russian - `L.language ru`\n\nAnd finally, if you have any issues with the bot, try `L.faq`. You can also just join the Lawliet Support server and ask for help:\n\n\uD83D\uDC49\uD83D\uDC49 [Lawliet Support Server](" + ExternalLinks.SERVER_INVITE_URL + ") \uD83D\uDC48\uD83D\uDC48";
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setAuthor(DiscordApiCollection.getInstance().getYourself())
                .setThumbnail(DiscordApiCollection.getInstance().getYourself().getAvatar())
                .setDescription(text);
        if (channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks())
            channel.sendMessage(eb).exceptionally(ExceptionLogger.get());
    }

}
