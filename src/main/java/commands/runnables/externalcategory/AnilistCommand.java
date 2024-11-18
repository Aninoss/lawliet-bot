package commands.runnables.externalcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.anilist.AnilistDownloader;
import modules.anilist.AnilistMedia;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "anilist",
        emoji = "⛩️",
        executableWithoutArgs = false,
        releaseDate = { 2024, 11, 25 },
        aliases = { "anime" }
)
public class AnilistCommand extends Command implements OnAlertListener {

    public AnilistCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        deferReply();
        AnilistMedia media = AnilistDownloader.getMediaBySearch(args, JDAUtil.channelIsNsfw(event.getChannel()));
        if (media != null) {
            EmbedBuilder eb = generateEmbed(media);
            EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, args);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    @NotNull
    @Override
    public AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(1, ChronoUnit.HOURS));
        AnilistMedia media;
        Integer previousHash = null;

        if (slot.getArgs().isEmpty()) {
            media = AnilistDownloader.getMediaBySearch(slot.getCommandKey(), JDAUtil.channelIsNsfw(slot.getGuildMessageChannel().get()));
        } else {
            String[] argsSplit = slot.getArgs().get().split("_");
            previousHash = Integer.parseInt(argsSplit[1]);
            media = AnilistDownloader.getMediaById(Integer.parseInt(argsSplit[0]), JDAUtil.channelIsNsfw(slot.getGuildMessageChannel().get()));
        }

        if (media == null) {
            if (slot.getArgs().isEmpty()) {
                EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, slot.getCommandKey());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(getLocale(), false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE_AND_SAVE;
            }
        }

        EmbedBuilder eb = generateEmbed(media);
        if (media.getStatus() == AnilistMedia.Status.NOT_YET_RELEASED || media.getStatus() == AnilistMedia.Status.RELEASING) {
            if (previousHash == null || media.hashCode() != previousHash) {
                slot.sendMessage(getLocale(), true, eb.build());
            }
            slot.setArgs(media.getId() + "_" + media.hashCode());
            return AlertResponse.CONTINUE_AND_SAVE;
        } else {
            EmbedUtil.addLog(eb, LogStatus.WARNING, getString("alertcompleted"));
            slot.sendMessage(getLocale(), true, eb.build());
            return AlertResponse.STOP_AND_DELETE;
        }
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

    private EmbedBuilder generateEmbed(AnilistMedia media) {
        StringBuilder episodesStringBuilder = new StringBuilder();
        if (media.getCurrentEpisode() != null) {
            episodesStringBuilder.append(media.getCurrentEpisode())
                    .append("/");
        }
        if (media.getTotalEpisodes() != null) {
            episodesStringBuilder.append(media.getTotalEpisodes());
        } else {
            episodesStringBuilder.append("?");
        }
        if (media.getNextEpisode() != null) {
            episodesStringBuilder.append("\n")
                    .append(getString("nextepisode", TimeFormat.DATE_TIME_SHORT.atInstant(media.getNextEpisode()).toString()));
        }

        return EmbedFactory.getEmbedDefault(this)
                .setTitle(StringUtil.shortenString(media.getTitle(), MessageEmbed.TITLE_MAX_LENGTH), media.getAnilistUrl())
                .setDescription(StringUtil.shortenString(media.getDescription(), 512))
                .setThumbnail(media.getCoverImage())
                .addField(getString("genres"), String.join(", ", media.getGenres()), true)
                .addField(getString("status"), getString("status_" + media.getStatus().name()), true)
                .addField(getString("episodes"), episodesStringBuilder.toString(), true)
                .addField(getString("score"), media.getAverageScore() + "%", true);
    }

}
