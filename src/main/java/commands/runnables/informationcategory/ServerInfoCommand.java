package commands.runnables.informationcategory;

import java.util.Locale;
import java.util.Optional;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "serverinfo",
        emoji = "\uD83D\uDC6A",
        executableWithoutArgs = true,
        aliases = { "serverinfos", "serverstat", "serverstats", "guild", "server" }
)
public class ServerInfoCommand extends Command {

    public ServerInfoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Guild guild = event.getGuild();

        long bots = guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();
        String[] argsArray = {
                StringUtil.escapeMarkdown(guild.getName()),
                guild.getId(),
                Optional.ofNullable(guild.getOwner()).map(owner -> StringUtil.escapeMarkdown(owner.getUser().getAsTag())).orElse("-"),
                guild.getRegion().getName(),
                TimeUtil.getInstantString(getLocale(), guild.getTimeCreated().toInstant(), true),
                guild.getIconUrl() != null ? guild.getIconUrl() : "-",
                StringUtil.numToString(guild.getMemberCount()),
                StringUtil.numToString(bots),
                StringUtil.numToString(guild.getMemberCount() - bots),
                StringUtil.numToString(guild.getBoostCount()),
                StringUtil.numToString(guild.getRoles().size()),
                StringUtil.numToString(guild.getChannels().stream().filter(channel -> channel.getType() == ChannelType.TEXT || channel.getType() == ChannelType.VOICE).count()),
                StringUtil.numToString(guild.getChannels().stream().filter(channel -> channel.getType() == ChannelType.TEXT).count()),
                StringUtil.numToString(guild.getChannels().stream().filter(channel -> channel.getType() == ChannelType.VOICE).count())
        };

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", argsArray));
        if (guild.getIconUrl() != null) {
            eb.setThumbnail(guild.getIconUrl());
        }

        event.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

}
