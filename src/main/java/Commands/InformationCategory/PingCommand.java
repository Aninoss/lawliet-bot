package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Core.CustomThread;
import Core.EmbedFactory;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.mention.AllowedMentions;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@CommandProperties(
        trigger = "ping",
        emoji = "\uD83C\uDFD3",
        executable = false
)
public class PingCommand extends Command {

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