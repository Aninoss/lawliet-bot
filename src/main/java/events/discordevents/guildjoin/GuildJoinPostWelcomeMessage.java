package events.discordevents.guildjoin;

import constants.ExternalLinks;
import core.EmbedFactory;
import core.ShardManager;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

@DiscordEvent
public class GuildJoinPostWelcomeMessage extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event) {
        JDAUtil.getFirstWritableChannelOfGuild(event.getGuild())
                .ifPresent(this::sendNewMessage);
        return true;
    }

    private void sendNewMessage(TextChannel channel) {
        String text = "Hi! Thanks for inviting me to your lovely server! ❤️\n\n• Just write `L.help` to get an overview of all my commands and features\n• You can restrict the channels which can trigger bot commands by running `L.whitelist`\n• With `L.fishery` you can configure the fishing idle-game / economy and read how it works\n\nFurthermore, you can also change the bot language:\n• \uD83C\uDDE9\uD83C\uDDEA German: `L.language de`\n• \uD83C\uDDF7\uD83C\uDDFA Russian: `L.language ru`\n\nAnd finally, if you have any issues with the bot, then you can take a look at the [FAQ Page](" + ExternalLinks.FAQ_WEBSITE + "). You can also just join the Lawliet Support server and ask for help:\n\n[Join Lawliet Support Server](" + ExternalLinks.SERVER_INVITE_URL + ")";
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(text)
                .setThumbnail(ShardManager.getInstance().getSelf().getEffectiveAvatarUrl());

        EmbedUtil.setMemberAuthor(eb, channel.getGuild().getSelfMember());
        if (BotPermissionUtil.canWriteEmbed(channel)) {
            channel.sendMessage(eb.build()).queue();
        }
    }

}
