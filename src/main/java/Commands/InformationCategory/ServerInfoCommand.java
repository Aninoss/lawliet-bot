package Commands.InformationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.Tools;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "serverinfo",
        emoji = "\uD83D\uDC6A",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/information-icon.png",
        executable = true,
        aliases = {"serverinfos"}
)
public class ServerInfoCommand extends Command implements onRecievedListener {

    public ServerInfoCommand() {
        super();
    }

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();

        String[] args = {
                server.getName(),
                server.getIdAsString(),
                server.getOwner() == null ? "-" : server.getOwner().getDiscriminatedName(),
                server.getRegion().getName(),
                Tools.getInstantString(getLocale(), server.getCreationTimestamp(), true),
                server.getIcon().isPresent() ? server.getIcon().get().getUrl().toString() : "-",
                Tools.numToString(getLocale(), server.getMembers().size()),
                Tools.numToString(getLocale(), server.getMembers().stream().filter(member -> !member.isBot()).count()),
                Tools.numToString(getLocale(), server.getMembers().stream().filter(User::isBot).count()),
                Tools.numToString(getLocale(), server.getRoles().size()),
                Tools.numToString(getLocale(), server.getChannels().size()),
                Tools.numToString(getLocale(), server.getChannels().stream().filter(channel -> channel.asServerTextChannel().isPresent()).count()),
                Tools.numToString(getLocale(), server.getChannels().stream().filter(channel -> channel.asServerVoiceChannel().isPresent()).count())
        };

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", args));
        if (server.getIcon().isPresent()) eb.setThumbnail(server.getIcon().get());

        event.getServerTextChannel().get().sendMessage(eb).get();
        return true;
    }

}
