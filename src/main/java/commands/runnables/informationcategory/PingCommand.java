package commands.runnables.informationcategory;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Instant startTime = (Instant) getAttachments().get("starting_time");
        long milisInternal = TimeUtil.getMillisBetweenInstants(startTime, Instant.now());
        long milisGateway = event.getJDA().getGatewayPing();
        long milisRest = event.getJDA().getRestPing().complete();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(
                "pong",
                StringUtil.numToString(milisInternal),
                StringUtil.numToString(milisGateway),
                StringUtil.numToString(milisRest)
        ));
        event.getChannel().sendMessage(eb.build()).queue();

        return true;
    }

}