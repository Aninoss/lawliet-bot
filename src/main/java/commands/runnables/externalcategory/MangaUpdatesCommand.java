package commands.runnables.externalcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.ListGen;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.mandaupdates.MangaUpdatesDownloader;
import modules.mandaupdates.MangaUpdatesRelease;
import modules.mandaupdates.MangaUpdatesSeries;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "mangaupdates",
        emoji = "📚",
        executableWithoutArgs = false,
        releaseDate = { 2022, 10, 8 },
        aliases = { "manga" }
)
public class MangaUpdatesCommand extends Command implements OnAlertListener {

    public MangaUpdatesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        deferReply();
        List<MangaUpdatesSeries> seriesList = MangaUpdatesDownloader.searchSeries(args);
        if (!seriesList.isEmpty()) {
            MangaUpdatesSeries series = seriesList.get(0);
            if (series.isNsfw() && !JDAUtil.channelIsNsfw(event.getChannel())) {
                drawMessageNew(EmbedFactory.getNSFWBlockEmbed(this)).exceptionally(ExceptionLogger.get());
                return false;
            } else {
                List<MangaUpdatesRelease> releases = MangaUpdatesDownloader.getReleasesOfSeries(seriesList.get(0).getSeriesId());
                EmbedBuilder eb = generateEmbed(series, releases, 5);
                EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
                drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                return true;
            }
        } else {
            EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, args);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    @NotNull
    @Override
    public AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
        MangaUpdatesSeries series;
        String recentRelease;

        if (slot.getArgs().isEmpty()) {
            List<MangaUpdatesSeries> seriesList = MangaUpdatesDownloader.searchSeries(slot.getCommandKey());
            if (!seriesList.isEmpty()) {
                series = seriesList.get(0);
                recentRelease = "";
            } else {
                EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, slot.getCommandKey());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(getLocale(), false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            }
        } else {
            JSONObject jsonObject = new JSONObject(slot.getArgs().get());
            long seriesId = jsonObject.getLong("series_id");
            String title = jsonObject.getString("title");
            String image = jsonObject.has("image") ? jsonObject.getString("image") : null;
            String url = jsonObject.getString("url");
            boolean nsfw = jsonObject.getBoolean("nsfw");
            recentRelease = jsonObject.getString("recent_release");
            series = new MangaUpdatesSeries(seriesId, title, image, url, nsfw);
        }

        if (series.isNsfw() && !JDAUtil.channelIsNsfw(slot.getGuildMessageChannel().get())) {
            EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale(), getPrefix());
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.sendMessage(getLocale(), false, eb.build());
            return AlertResponse.STOP_AND_DELETE;
        }

        List<MangaUpdatesRelease> releases = MangaUpdatesDownloader.getReleasesOfSeries(series.getSeriesId());

        int maxSlots = 5;
        for (int i = 0; i < Math.min(maxSlots, releases.size()); i++) {
            if (releases.get(i).getId().equals(recentRelease)) {
                maxSlots = i;
                break;
            }
        }

        if ((maxSlots > 0 && !releases.isEmpty()) || slot.getArgs().isEmpty()) {
            EmbedBuilder eb = generateEmbed(series, releases, maxSlots);
            slot.sendMessage(getLocale(), true, eb.build());
        }

        recentRelease = releases.isEmpty() ? "" : releases.get(0).getId();
        slot.setArgs(serializeSeriesForAlert(series, recentRelease).toString());
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

    private EmbedBuilder generateEmbed(MangaUpdatesSeries series, List<MangaUpdatesRelease> releases, int maxSlots) {
        List<MangaUpdatesRelease> releasesCapped = releases.subList(0, Math.min(maxSlots, releases.size()));
        String recentReleases = new ListGen<MangaUpdatesRelease>()
                .getList(releasesCapped, getString("noreleases"), release -> {
                            String chapter = release.getChapter().startsWith(series.getTitle())
                                    ? release.getChapter().substring(series.getTitle().length()).trim()
                                    : release.getChapter();
                            String scanlator = StringUtil.escapeMarkdown(release.getScanlator());
                            if (!chapter.isBlank()) {
                                return getString("release", StringUtil.escapeMarkdown(chapter), scanlator);
                            } else {
                                return getString("release_nochapter", scanlator);
                            }
                        }
                );

        String viewMore = releases.size() > maxSlots ? getString("viewmore", series.getReleasesUrl()) : "";
        recentReleases = StringUtil.shortenStringLine(recentReleases, MessageEmbed.VALUE_MAX_LENGTH - viewMore.length());
        recentReleases += viewMore;

        return EmbedFactory.getEmbedDefault(this)
                .setTitle(series.getTitle(), series.getUrl())
                .setThumbnail(series.getImage())
                .addField(getString("recentreleases"), recentReleases, false);
    }

    private JSONObject serializeSeriesForAlert(MangaUpdatesSeries series, String recentRelease) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("series_id", series.getSeriesId());
        jsonObject.put("title", series.getTitle());
        jsonObject.put("image", series.getImage());
        jsonObject.put("url", series.getUrl());
        jsonObject.put("nsfw", series.isNsfw());
        jsonObject.put("recent_release", recentRelease);
        return jsonObject;
    }

}
