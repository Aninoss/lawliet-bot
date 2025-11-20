package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.ExceptionLogger;
import core.utils.ComponentsUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Locale;

@CommandProperties(
        trigger = "ping",
        emoji = "\uD83C\uDFD3",
        executableWithoutArgs = true
)
public class PingCommand extends Command {

    public PingCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws InterruptedException {
        Instant startTime = getAttachment("starting_time", Instant.class);
        long millisInternal = TimeUtil.getMillisBetweenInstants(startTime, Instant.now());
        long millisGateway = event.getJDA().getGatewayPing();
        long millisRest = event.getJDA().getRestPing().complete();

        TextDisplay content = TextDisplay.of(getString(
                "pong",
                StringUtil.numToString(millisInternal),
                StringUtil.numToString(millisGateway),
                StringUtil.numToString(millisRest)
        ));
        drawMessageNew(ComponentsUtil.createCommandComponentTree(this, content))
                .exceptionally(ExceptionLogger.get());

        return true;
    }

}