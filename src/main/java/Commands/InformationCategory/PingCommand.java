package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Core.Clock;
import Core.CustomThread;
import Core.EmbedFactory;
import Core.Tools.StringTools;
import Core.Tools.TimeTools;
import MySQL.Modules.Survey.DBSurvey;
import MySQL.Modules.Survey.SurveyBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "ping",
        emoji = "\uD83C\uDFD3",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/arrow-refresh-4-icon.png",
        executable = false
)
public class PingCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Instant creationTime = ((CustomThread)Thread.currentThread()).getCreationTime();

        long milisInternal = TimeTools.getMilisBetweenInstants(creationTime, Instant.now());

        Instant startTime = Instant.now();
        Message message = event.getServerTextChannel().get().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("pong_start", StringTools.numToString(getLocale(), milisInternal)))).get();
        Instant endTime = Instant.now();

        long milisDiscordServers = TimeTools.getMilisBetweenInstants(startTime, endTime);
        message.edit(EmbedFactory.getCommandEmbedStandard(this, getString("pong_end", StringTools.numToString(getLocale(), milisInternal), StringTools.numToString(getLocale(), milisDiscordServers)))).get();

        return true;
    }

}