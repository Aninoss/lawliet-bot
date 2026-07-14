package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "channelinfo",
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "channelinfos", "channelstat", "channelstats", "channel" }
)
public class ChannelInfoCommand extends Command {

    public ChannelInfoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        boolean noMention = false;
        Guild guild = event.getGuild();
        ArrayList<GuildMessageChannel> list = new ArrayList<>(MentionUtil.getGuildMessageChannels(event.getGuild(), args).getList());
        if (list.isEmpty()) {
            list.add(event.getMessageChannel());
            noMention = true;
        }

        GuildMessageChannel channel = list.get(0);
        List<Member> members = JDAUtil.getChannelMembers(channel);

        String[] argsArray = {
                StringUtil.escapeMarkdown(channel.getName()),
                channel.getId(),
                TimeFormat.DATE_TIME_SHORT.atInstant(channel.getTimeCreated().toInstant()).toString(),
                StringUtil.numToString(members.size()),
                StringUtil.numToString(members.stream().filter(member -> !member.getUser().isBot()).count()),
                StringUtil.numToString(members.stream().filter(member -> member.getUser().isBot()).count())
        };

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", argsArray));
        if (guild.getIconUrl() != null) {
            eb.setThumbnail(guild.getIconUrl());
        }

        if (noMention) {
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "channel_mention_optional"));
            if (!args.isEmpty()) {
                EmbedUtil.addNoResultsLog(eb, getLocale(), args);
            }
        }

        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
