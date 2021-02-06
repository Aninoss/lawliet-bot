package commands.runnables.externalcategory;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.Response;
import core.EmbedFactory;
import core.ResourceHandler;
import core.TextManager;
import core.internet.HttpRequest;
import core.internet.HttpResponse;
import core.utils.StringUtil;
import modules.YouTubePlayer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getNoResultsString(getLocale(), followedString))).get();
            return false;
        }

        AudioTrackInfo meta = metaOpt.get();
        if (meta.length >= MINUTES_CAP * 60_000) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("toolong", String.valueOf(MINUTES_CAP)))).get();
            return false;
        }

        Message message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("loading", StringUtil.escapeMarkdownInField(meta.title), StringUtil.getLoadingReaction(event.getServerTextChannel().get())))).get();

        if (sendApiRequest("https://www.youtube.com/watch?v=" + meta.identifier)) {
            Pattern filePattern = Pattern.compile(String.format(".*\\[%s\\]\\.[A-Za-z0-9]*$", Pattern.quote(meta.identifier)));
            for (int i = 0; i < 500; i++) {
                Thread.sleep(100);
                List<File> validFiles = getValidFiles(ResourceHandler.getFileResource("data/youtube-dl"), meta.identifier, filePattern);

                if (validFiles.size() == 1 && validFiles.get(0).getAbsolutePath().endsWith(".mp3")) {
                    return handleFile(event, message, meta, validFiles.get(0));
                }
            }
        }

        event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                getString("error"),
                TextManager.getString(getLocale(), TextManager.GENERAL, "error")
        )).get();
        return false;
    }

    private List<File> getValidFiles(File root, String videoId, Pattern filePattern) {
        return Arrays.stream(Objects.requireNonNull(root.listFiles()))
                .filter(file -> filePattern.matcher(file.getAbsolutePath()).matches())
                .collect(Collectors.toUnmodifiableList());
    }

    private boolean sendApiRequest(String url) throws ExecutionException, InterruptedException {
        String body = String.format("url=%s&format=mp3", URLEncoder.encode(url, StandardCharsets.UTF_8));
        HttpResponse response = HttpRequest.getData("http://youtube-dl:8080/youtube-dl/q", body).get();
        return response
                .getContent()
                .map(data -> new JSONObject(data).getBoolean("success"))
                .orElse(false);
    }

    private boolean handleFile(MessageCreateEvent event, Message message, AudioTrackInfo meta, File mp3File) throws InterruptedException {
        try {
            event.getMessage().getUserAuthor().get()
                    .sendMessage(getString("success_dm", StringUtil.escapeMarkdownInField(meta.title), StringUtil.escapeMarkdownInField(meta.author)), mp3File).get();
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