package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import CommandSupporters.CommandLogger.CommandLogger;
import Constants.Settings;
import General.Clock;
import General.EmbedFactory;
import General.TextManager;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Duration;
import java.time.Instant;

@CommandProperties(
        trigger = "ping",
        emoji = "\uD83C\uDFD3",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/arrow-refresh-4-icon.png",
        executable = false
)
public class PingCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Instant startTime = event.getMessage().getCreationTimestamp();
        Message message = event.getServerTextChannel().get().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("pong_notime"))).get();
        Instant endTime = Instant.now();

        Duration duration = Duration.between(startTime, endTime);
        message.edit(EmbedFactory.getCommandEmbedStandard(this, getString("pong", String.valueOf((Math.abs(duration.getSeconds() * 1000000000) + Math.abs(duration.getNano())) / 1000000)))).get();

        return true;
    }

}