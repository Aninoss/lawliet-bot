package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.Mention.MentionUtil;
import Core.TextManager;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "channelinfo",
        emoji = "\uD83D\uDCDD",
        executable = true,
        aliases = {"channelinfos", "channelstat", "channelstats"}
)
public class ChannelInfoCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        boolean noMention = false;
        Server server = event.getServer().get();
        ArrayList<ServerTextChannel> list = MentionUtil.getTextChannels(event.getMessage(), followedString).getList();
        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_channels"))).get();
            return false;
        } else if (list.size() == 0) {
            list.add(event.getServerTextChannel().get());
            noMention = true;
        }

        for(ServerTextChannel channel: list) {
            List<User> members = channel.getServer().getMembers().stream().filter(channel::canSee).collect(Collectors.toList());

            String[] args = {
                    channel.getName(),
                    channel.getIdAsString(),
                    TimeUtil.getInstantString(getLocale(), channel.getCreationTimestamp(), true),
                    StringUtil.numToString(getLocale(), members.size()),
                    StringUtil.numToString(getLocale(), members.stream().filter(member -> !member.isBot()).count()),
                    StringUtil.numToString(getLocale(), members.stream().filter(User::isBot).count())
            };

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", args));
            if (server.getIcon().isPresent()) eb.setThumbnail(server.getIcon().get());

            if (noMention) {
                eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "channel_mention_optional"));
                if (followedString.length() > 0)
                    EmbedFactory.addNoResultsLog(eb, getLocale(), followedString);
            }

            event.getServerTextChannel().get().sendMessage(eb).get();
        }
        return true;
    }

}
