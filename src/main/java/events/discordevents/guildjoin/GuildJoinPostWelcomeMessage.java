package events.discordevents.guildjoin;

import constants.ExternalLinks;
import core.EmbedFactory;
import core.ExceptionLogger;
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
        String text = """
                      Hi! Thanks for inviting me to your lovely server! ❤️
                      
                      *No dashboard is required! You can set up every function within your Discord client by running the corresponding command.*
                              
                      • Just write `L.help` to get an overview of all my commands and features
                      • You can restrict the channels which can trigger bot commands by running `L.whitelist`
                      • With `L.fishery` you can configure the fishing idle-game / economy and read how it works
                              
                      Furthermore, you can also change the bot language:
                      • 🇩🇪 German: `L.language de`
                      • 🇪🇸 Spanish: `L.language es`
                      • 🇷🇺 Russian: `L.language ru`
                              
                      And finally, if you have any issues with the bot, then you can take a look at the [FAQ page](%s). You can also just join the Lawliet support server and ask for help:
                              
                      [Join Lawliet Support Server](%s)
                      """;

        text = String.format(text, ExternalLinks.FAQ_WEBSITE, ExternalLinks.SERVER_INVITE_URL);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(text)
                .setThumbnail(ShardManager.getInstance().getSelf().getEffectiveAvatarUrl());

        EmbedUtil.setMemberAuthor(eb, channel.getGuild().getSelfMember());
        if (BotPermissionUtil.canWriteEmbed(channel)) {
            channel.sendMessageEmbeds(eb.build()).submit()
                    .exceptionally(ExceptionLogger.get("50013"));
        }
    }

}
