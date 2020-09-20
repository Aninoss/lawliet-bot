package commands.runnables.informationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
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
        long milisGateway = event.getApi().getLatestGatewayLatency().toMillis();
        long milisRest = event.getApi().measureRestLatency().get().toMillis();

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("pong",
                StringUtil.numToString(getLocale(), milisInternal),
                StringUtil.numToString(getLocale(), milisGateway),
                StringUtil.numToString(getLocale(), milisRest)
        ));
        event.getChannel().sendMessage(eb);

        return true;
    }

}