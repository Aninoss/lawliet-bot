package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;

import commands.Command;
import core.EmbedFactory;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "serverinfo",
        emoji = "\uD83D\uDC6A",
        executable = true,
        aliases = {"serverinfos", "serverstat", "serverstats"}
)
public class ServerInfoCommand extends Command {

    public ServerInfoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();

        String[] args = {
                StringUtil.escapeMarkdown(server.getName()),
                server.getIdAsString(),
                StringUtil.escapeMarkdown(server.getOwner().getDiscriminatedName()),
                server.getRegion().getName(),
                TimeUtil.getInstantString(getLocale(), server.getCreationTimestamp(), true),
                server.getIcon().isPresent() ? server.getIcon().get().getUrl().toString() : "-",
                StringUtil.numToString(getLocale(), server.getMemberCount()),
                StringUtil.numToString(getLocale(), server.getMembers().stream().filter(member -> !member.isBot()).count()),
                StringUtil.numToString(getLocale(), server.getMembers().stream().filter(User::isBot).count()),
                StringUtil.numToString(getLocale(), server.getBoostCount()),
                StringUtil.numToString(getLocale(), server.getRoles().size()),
                StringUtil.numToString(getLocale(), server.getChannels().stream().filter(channel -> channel.asServerTextChannel().isPresent() || channel.asServerVoiceChannel().isPresent()).count()),
                StringUtil.numToString(getLocale(), server.getChannels().stream().filter(channel -> channel.asServerTextChannel().isPresent()).count()),
                StringUtil.numToString(getLocale(), server.getChannels().stream().filter(channel -> channel.asServerVoiceChannel().isPresent()).count())
        };

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", args));
        if (server.getIcon().isPresent()) eb.setThumbnail(server.getIcon().get());

        event.getServerTextChannel().get().sendMessage(eb).get();
        return true;
    }

}
