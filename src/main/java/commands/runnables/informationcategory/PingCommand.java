package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;
import commands.Command;
import core.EmbedFactory;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.util.Locale;

@CommandProperties(
        trigger = "ping",
        emoji = "\uD83C\uDFD3",
        executable = true
)
public class PingCommand extends Command {

    public PingCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Instant creationTime = getStartTime();

        long milisInternal = TimeUtil.getMilisBetweenInstants(creationTime, Instant.now());

        Instant startTime = Instant.now();
        Message message = event.getServerTextChannel().get().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("pong_start", StringUtil.numToString(getLocale(), milisInternal)))).get();
        Instant endTime = Instant.now();

        long milisDiscordServers = TimeUtil.getMilisBetweenInstants(startTime, endTime);
        message.edit(EmbedFactory.getCommandEmbedStandard(this, getString("pong_end", StringUtil.numToString(getLocale(), milisInternal), StringUtil.numToString(getLocale(), milisDiscordServers)))).get();

        return true;
    }

}