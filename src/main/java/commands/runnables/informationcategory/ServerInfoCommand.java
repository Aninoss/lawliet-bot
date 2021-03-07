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
        executableWithoutArgs = true,
        aliases = {"serverinfos", "serverstat", "serverstats"}
)
public class ServerInfoCommand extends Command {

    public ServerInfoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Server server = event.getServer().get();

        String[] args = {
                StringUtil.escapeMarkdown(server.getName()),
                server.getIdAsString(),
                server.getOwner().map(owner -> StringUtil.escapeMarkdown(owner.getDiscriminatedName())).orElse("-"),
                server.getRegion().getName(),
                TimeUtil.getInstantString(getLocale(), server.getCreationTimestamp(), true),
                server.getIcon().isPresent() ? server.getIcon().get().getUrl().toString() : "-",
                StringUtil.numToString(server.getMemberCount()),
                StringUtil.numToString(server.getMembers().stream().filter(member -> !member.isBot()).count()),
                StringUtil.numToString(server.getMembers().stream().filter(User::isBot).count()),
                StringUtil.numToString(server.getBoostCount()),
                StringUtil.numToString(server.getRoles().size()),
                StringUtil.numToString(server.getChannels().stream().filter(channel -> channel.asServerTextChannel().isPresent() || channel.asServerVoiceChannel().isPresent()).count()),
                StringUtil.numToString(server.getChannels().stream().filter(channel -> channel.asServerTextChannel().isPresent()).count()),
                StringUtil.numToString(server.getChannels().stream().filter(channel -> channel.asServerVoiceChannel().isPresent()).count())
        };

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", args));
        if (server.getIcon().isPresent()) eb.setThumbnail(server.getIcon().get());

        event.getServerTextChannel().get().sendMessage(eb).get();
        return true;
    }

}
