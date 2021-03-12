package commands.runnables.externalcategory;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.MainLogger;
import core.ResourceHandler;
import core.TextManager;
import core.internet.HttpRequest;
import core.internet.HttpResponse;
import core.utils.JDAEmojiUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.YouTubeMeta;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        args = args.replace("<", "").replace(">", "");

        if (args.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args")).build())
                    .queue();
            return false;
        }

        if (args.contains("&")) {
            args = args.split("&")[0];
        }

        Optional<AudioTrackInfo> metaOpt = YouTubeMeta.getInstance().getFromVideoURL(args);
        if (metaOpt.isEmpty() || metaOpt.get().isStream) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getNoResultsString(getLocale(), args)).build()).queue();
            return false;
        }

        AudioTrackInfo meta = metaOpt.get();
        if (meta.length >= MINUTES_CAP * 60_000) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("toolong", String.valueOf(MINUTES_CAP))).build()).queue();
            return false;
        }

        Message message = event.getChannel().sendMessage(
                EmbedFactory.getEmbedDefault(
                        this,
                        getString(
                                "loading",
                                StringUtil.escapeMarkdownInField(meta.title),
                                JDAEmojiUtil.getLoadingEmojiMention(event.getChannel())
                        )
                ).build()
        ).complete();

        if (sendApiRequest("https://www.youtube.com/watch?v=" + meta.identifier)) {
            Pattern filePattern = Pattern.compile(String.format(".*\\[%s\\]\\.[A-Za-z0-9]*$", Pattern.quote(meta.identifier)));
            for (int i = 0; i < 500; i++) {
                Thread.sleep(100);
                List<File> validFiles = getValidFiles(ResourceHandler.getFileResource("data/youtube-dl"), filePattern);

                if (validFiles.size() == 1 && validFiles.get(0).getAbsolutePath().endsWith(".mp3")) {
                    handleFile(event, message, meta, validFiles.get(0));
                    return true;
                }
            }
        }

        event.getChannel().sendMessage(EmbedFactory.getEmbedError(
                this,
                getString("error"),
                TextManager.getString(getLocale(), TextManager.GENERAL, "error")
        ).build()).queue();
        return false;
    }

    private List<File> getValidFiles(File root, Pattern filePattern) {
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

    private void handleFile(GuildMessageReceivedEvent event, Message message, AudioTrackInfo meta, File mp3File) {
        JDAUtil.sendPrivateMessage(event.getMember().getIdLong(), privateChannel -> {
            return privateChannel.sendMessage(getString("success_dm", StringUtil.escapeMarkdownInField(meta.title), StringUtil.escapeMarkdownInField(meta.author)))
                    .addFile(mp3File);
        }).queue(m -> {
            mp3File.delete();
            message.editMessage(EmbedFactory.getEmbedDefault(this, getString("success")).build()).queue();
        }, e -> {
            MainLogger.get().error("Ytmp3 Error", e);
            mp3File.delete();
            message.editMessage(
                    EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_dms"), TextManager.getString(getLocale(), TextManager.GENERAL, "error")).build()
            ).queue();
        });
    }

}