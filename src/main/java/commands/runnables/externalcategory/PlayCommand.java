package commands.runnables.externalcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import modules.YouTubePlayer;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.event.message.MessageCreateEvent;
import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "play",
        emoji = "\uD83D\uDDBC",
        withLoadingBar = true,
        executableWithoutArgs = false,
        exlusiveUsers = { 272037078919938058L },
        aliases = { "start" }
)
public class PlayCommand extends Command {

    public PlayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Optional<ServerVoiceChannel> voiceChannelOpt = event.getServer().get().getConnectedVoiceChannel(event.getMessageAuthor().asUser().get());
        if (voiceChannelOpt.isPresent()) {
            ServerVoiceChannel voiceChannel = voiceChannelOpt.get();
            AudioConnection audioConnection = voiceChannel.connect().get();
            YouTubePlayer.getInstance().play(audioConnection, followedString);
            return true;
        }

        return true;
    }

}
