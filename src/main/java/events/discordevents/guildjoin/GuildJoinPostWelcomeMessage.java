package events.discordevents.guildjoin;

import constants.ExternalLinks;
import core.EmbedFactory;
import core.ShardManager;
import core.utils.DiscordUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.server.ServerJoinEvent;

@DiscordEvent
public class GuildJoinPostWelcomeMessage extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(ServerJoinEvent event) throws Throwable {
        DiscordUtil.getFirstWritableChannelOfServer(event.getServer()).ifPresent(this::sendNewMessage);
        return true;
    }

    private void sendNewMessage(ServerTextChannel channel) {
        String text = "Hi! Thanks for inviting me to your lovely server! ❤️\n\n• Just write `L.help` to get an overview of all my commands and features\n• You can restrict the channels which can trigger bot commands by running `L.whitelist`\n• With `L.fishery` you can configure the fishing idle-game / economy and read how it works\n\nFurthermore, you can also change the bot language:\n• \uD83C\uDDE9\uD83C\uDDEA German: `L.language de`\n• \uD83C\uDDF7\uD83C\uDDFA Russian: `L.language ru`\n\nAnd finally, if you have any issues with the bot, then you can take a look at the [FAQ Page](" + ExternalLinks.FAQ_WEBSITE + "). You can also just join the Lawliet Support server and ask for help:\n\n[Join Lawliet Support Server](" + ExternalLinks.SERVER_INVITE_URL + ")";
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setAuthor(ShardManager.getInstance().getSelf())
                .setThumbnail(ShardManager.getInstance().getSelf().getAvatar())
                .setDescription(text);
        if (channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks())
            channel.sendMessage(eb);
    }

}
