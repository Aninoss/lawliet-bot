package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "serverinfo",
        emoji = "👪",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "serverinfos", "serverstat", "serverstats", "guild", "server" }
)
public class ServerInfoCommand extends Command {

    public ServerInfoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        Guild guild = event.getGuild();

        long bots = guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();
        String[] argsArray = {
                StringUtil.escapeMarkdown(guild.getName()),
                guild.getId(),
                Optional.ofNullable(guild.getOwner()).map(owner -> StringUtil.escapeMarkdown(owner.getUser().getAsTag())).orElse("-"),
                "",
                TimeFormat.DATE_TIME_SHORT.atInstant(guild.getTimeCreated().toInstant()).toString(),
                guild.getIconUrl() != null ? guild.getIconUrl() : "-",
                StringUtil.numToString(guild.getMemberCount()),
                StringUtil.numToString(guild.getMemberCount() - bots),
                StringUtil.numToString(bots),
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

        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
