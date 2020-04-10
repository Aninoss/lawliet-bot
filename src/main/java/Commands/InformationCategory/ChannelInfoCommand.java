package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.Mention.MentionTools;
import Core.TextManager;
import Core.Tools.StringTools;
import Core.Tools.TimeTools;
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
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/information-icon.png",
        executable = true,
        aliases = {"channelinfos", "channelstat", "channelstats"}
)
public class ChannelInfoCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        boolean noMention = false;
        Server server = event.getServer().get();
        ArrayList<ServerTextChannel> list = MentionTools.getTextChannels(event.getMessage(), followedString).getList();
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
                    TimeTools.getInstantString(getLocale(), channel.getCreationTimestamp(), true),
                    StringTools.numToString(getLocale(), members.size()),
                    StringTools.numToString(getLocale(), members.stream().filter(member -> !member.isBot()).count()),
                    StringTools.numToString(getLocale(), members.stream().filter(User::isBot).count())
            };

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", args));
            if (server.getIcon().isPresent()) eb.setThumbnail(server.getIcon().get());
            if (noMention) eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "channel_mention_optional"));

            event.getServerTextChannel().get().sendMessage(eb).get();
        }
        return true;
    }

}
