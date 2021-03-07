package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;

import commands.Command;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.TextManager;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "channelinfo",
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = true,
        aliases = {"channelinfos", "channelstat", "channelstats"}
)
public class ChannelInfoCommand extends Command {

    public ChannelInfoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        boolean noMention = false;
        Server server = event.getServer().get();
        ArrayList<ServerTextChannel> list = MentionUtil.getTextChannels(event.getMessage(), followedString).getList();
        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_channels"))).get();
            return false;
        } else if (list.size() == 0) {
            list.add(event.getServerTextChannel().get());
            noMention = true;
        }

        for(ServerTextChannel channel: list) {
            List<User> members = channel.getServer().getMembers().stream().filter(channel::canSee).collect(Collectors.toList());

            String[] args = {
                    StringUtil.escapeMarkdown(channel.getName()),
                    channel.getIdAsString(),
                    TimeUtil.getInstantString(getLocale(), channel.getCreationTimestamp(), true),
                    StringUtil.numToString(members.size()),
                    StringUtil.numToString(members.stream().filter(member -> !member.isBot()).count()),
                    StringUtil.numToString(members.stream().filter(User::isBot).count())
            };

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", args));
            if (server.getIcon().isPresent()) eb.setThumbnail(server.getIcon().get());

            if (noMention) {
                EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "channel_mention_optional"));
                if (followedString.length() > 0)
                    EmbedUtil.addNoResultsLog(eb, getLocale(), followedString);
            }

            event.getServerTextChannel().get().sendMessage(eb).get();
        }
        return true;
    }

}
