package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Core.CustomThread;
import Core.EmbedFactory;
import Core.Utils.InternetUtil;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@CommandProperties(
        trigger = "ping",
        emoji = "\uD83C\uDFD3",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/arrow-refresh-4-icon.png",
        executable = false
)
public class PingCommand extends Command {

    final Logger LOGGER = LoggerFactory.getLogger(PingCommand.class);

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Instant creationTime = ((CustomThread)Thread.currentThread()).getCreationTime();

        long milisInternal = TimeUtil.getMilisBetweenInstants(creationTime, Instant.now());

        Instant startTime = Instant.now();
        Message message = event.getServerTextChannel().get().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("pong_start", StringUtil.numToString(getLocale(), milisInternal)))).get();
        Instant endTime = Instant.now();

        long milisDiscordServers = TimeUtil.getMilisBetweenInstants(startTime, endTime);
        message.edit(EmbedFactory.getCommandEmbedStandard(this, getString("pong_end", StringUtil.numToString(getLocale(), milisInternal), StringUtil.numToString(getLocale(), milisDiscordServers)))).get();

        return true;
    }

}