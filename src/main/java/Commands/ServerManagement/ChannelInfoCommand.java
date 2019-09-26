package Commands.ServerManagement;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.Mention.MentionFinder;
import General.TextManager;
import General.Tools;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandProperties(
        trigger = "channelinfo",
        emoji = "\uD83D\uDCDD",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/information-icon.png",
        executable = true,
        aliases = {"channelinfos"}
)
public class ChannelInfoCommand extends Command implements onRecievedListener {

    public ChannelInfoCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        boolean noMention = false;
        Server server = event.getServer().get();
        ArrayList<ServerTextChannel> list = MentionFinder.getTextChannels(event.getMessage(), followedString).getList();
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
                    Tools.getInstantString(getLocale(), channel.getCreationTimestamp(), true),
                    Tools.numToString(getLocale(), members.size()),
                    Tools.numToString(getLocale(), members.stream().filter(member -> !member.isBot()).count()),
                    Tools.numToString(getLocale(), members.stream().filter(User::isBot).count())
            };

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", args)).
                    setThumbnail(server.getIcon().get());
            if (noMention) eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "channel_mention_optional"));

            event.getServerTextChannel().get().sendMessage(eb).get();
        }
        return true;
    }

}
