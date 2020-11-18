package commands.runnables.externalcategory;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import commands.Command;
import commands.listeners.CommandProperties;
import core.Bot;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import core.utils.SystemUtil;
import modules.YouTubePlayer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "ytmp3",
        emoji = "\uD83D\uDCE5",
        executableWithoutArgs = false,
        patreonRequired = true,
        maxCalculationTimeSec = 60,
        aliases = { "youtube", "yt", "youtubemp3" }
)
public class YouTubeMP3Command extends Command {

    private final static int MINUTES_CAP = 30;

    public YouTubeMP3Command(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        followedString = followedString.replace("<", "").replace(">", "");

        if (followedString.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"))).get();
            return false;
        }

        if (followedString.contains("&"))
            followedString = followedString.split("&")[0];

        Optional<AudioTrackInfo> metaOpt = YouTubePlayer.getInstance().meta(followedString);
        if (metaOpt.isEmpty() || metaOpt.get().isStream) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString))).get();
            return false;
        }

        AudioTrackInfo meta = metaOpt.get();
        if (meta.length >= MINUTES_CAP * 60_000) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("toolong", String.valueOf(MINUTES_CAP)))).get();
            return false;
        }

        Message message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("loading", StringUtil.escapeMarkdownInField(meta.title), StringUtil.getLoadingReaction(event.getServerTextChannel().get())))).get();
        SystemUtil.executeProcess(Bot.isProductionMode() ? "./ytmp3.sh" : "ytmp3.bat", meta.identifier);

        File mp3File = new File(String.format("temp/%s.mp3", meta.identifier));
        if (!mp3File.exists()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    getString("error"),
                    TextManager.getString(getLocale(), TextManager.GENERAL, "error")
            )).get();
            return false;
        }

        return handleFile(event, message, meta, mp3File);
    }

    private boolean handleFile(MessageCreateEvent event, Message message, AudioTrackInfo meta, File mp3File) throws InterruptedException {
        String newFileName = meta.title.replace(" ", "_").replaceAll("\\W+", "");
        File newMp3File = new File(String.format("temp/%s.mp3", newFileName));

        if (newFileName.length() > 0 && mp3File.renameTo(newMp3File))
            mp3File = newMp3File;

        try {
            event.getMessage().getUserAuthor().get().sendMessage(getString("success_dm", StringUtil.escapeMarkdownInField(meta.title), StringUtil.escapeMarkdownInField(meta.author)), mp3File).get();
        } catch (ExecutionException e) {
            mp3File.delete();
            message.edit(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "no_dms"),
                    TextManager.getString(getLocale(), TextManager.GENERAL, "error")
            )).exceptionally(ExceptionLogger.get());
            return false;
        }

        mp3File.delete();
        message.edit(EmbedFactory.getEmbedDefault(this, getString("success"))).exceptionally(ExceptionLogger.get());
        return true;
    }

}