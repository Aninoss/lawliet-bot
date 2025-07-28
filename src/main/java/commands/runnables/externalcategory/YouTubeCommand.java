package commands.runnables.externalcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MainLogger;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.schedulers.AlertResponse;
import modules.youtube.YouTubeDownloader;
import modules.youtube.YouTubeVideo;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "youtube",
        emoji = "📺",
        executableWithoutArgs = false,
        aliases = {"yt"}
)
public class YouTubeCommand extends Command implements OnAlertListener {

    private static final String YOUTUBE_ICON = "https://cdn.discordapp.com/attachments/499629904380297226/1270375979436478526/youtube.png";

    public YouTubeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException, IOException {
        deferReply();

        List<YouTubeVideo> videos;
        try {
            videos = YouTubeDownloader.retrieveVideos(args);
        } catch (IOException e) {
            MainLogger.get().error("YouTube exception", e);
            EmbedBuilder eb = EmbedFactory.getApiDownEmbed(this, "youtube.com");
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        if (videos == null || videos.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, args);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        EmbedBuilder eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), getEmbed(videos.get(0), true), getPrefix(), getTrigger());
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));

        ArrayList<YouTubeVideo> videos = YouTubeDownloader.retrieveVideos(slot.getCommandKey());
        if (videos == null) {
            if (slot.getArgs().isEmpty()) {
                EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, slot.getCommandKey());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(getLocale(), false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            } else {
                slot.setNextRequest(Instant.now().plus(1, ChronoUnit.HOURS));
                return AlertResponse.CONTINUE_AND_SAVE;
            }
        }
        if (videos.isEmpty()) {
            return AlertResponse.CONTINUE_AND_SAVE;
        }
        Collections.reverse(videos);

        ArrayList<String> idList = slot.getArgs().isPresent() ? new ArrayList<>(List.of(slot.getArgs().get().split(","))) : new ArrayList<>();
        if (idList.size() == 1 && StringUtil.stringIsInstant(idList.get(0))) {
            List<String> newIdList = videos.stream().map(YouTubeVideo::getId).collect(Collectors.toList());
            slot.setArgs(StringUtils.join(newIdList, ","));
            return AlertResponse.CONTINUE_AND_SAVE;
        }

        List<MessageEmbed> embedList = videos.stream()
                .filter(video -> !idList.contains(video.getId()))
                .peek(video -> idList.add(video.getId()))
                .map(post -> getEmbed(post, false).build())
                .collect(Collectors.toList());
        if (slot.getArgs().isEmpty()) {
            embedList = List.of(embedList.get(embedList.size() - 1));
        }
        if (!embedList.isEmpty()) {
            slot.sendMessage(getLocale(), true, embedList);
        }

        while (idList.size() > 250) {
            idList.remove(0);
        }
        slot.setArgs(StringUtils.join(idList, ","));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

    private EmbedBuilder getEmbed(YouTubeVideo video, boolean includeStatistics) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setAuthor(video.getCreator(), video.getCreatorUrl(), YOUTUBE_ICON)
                .setTitle(video.getTitle(), video.getLink())
                .setImage(video.getThumbnail())
                .setTimestamp(video.getPublicationTime());

        if (includeStatistics) {
            if (video.getLikes() != null) {
                eb.setFooter(getString("footer", StringUtil.numToString(video.getViews()), StringUtil.numToString(video.getLikes())));
            } else {
                eb.setFooter(getString("footer_nolikes", StringUtil.numToString(video.getViews())));
            }
        }
        return eb;
    }

}
