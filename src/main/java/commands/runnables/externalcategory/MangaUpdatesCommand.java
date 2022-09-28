package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.ListGen;
import core.components.ActionRows;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.mandaupdates.MangaUpdatesDownloader;
import modules.mandaupdates.MangaUpdatesRelease;
import modules.mandaupdates.MangaUpdatesSeries;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

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
        event.deferReply();
        List<MangaUpdatesSeries> seriesList = MangaUpdatesDownloader.searchSeries(args);
        if (!seriesList.isEmpty()) {
            MangaUpdatesSeries series = seriesList.get(0);
            if (series.isNsfw() && !event.getTextChannel().isNSFW()) {
                setActionRows(ActionRows.of(EmbedFactory.getNSFWBlockButton(getLocale())));
                drawMessageNew(EmbedFactory.getNSFWBlockEmbed(this)).exceptionally(ExceptionLogger.get());
                return false;
            } else {
                List<MangaUpdatesRelease> releases = MangaUpdatesDownloader.getReleasesOfSeries(seriesList.get(0).getSeriesId());
                EmbedBuilder eb = generateEmbed(series, releases, 5);
                drawMessageNew(eb);
                return true;
            }
        } else {
            EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, args);
            drawMessageNew(eb);
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
                slot.sendMessage(false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            }
        } else {
            JSONObject jsonObject = new JSONObject(slot.getArgs().get());
            long seriesId = jsonObject.getLong("series_id");
            String title = jsonObject.getString("title");
            String image = jsonObject.getString("image");
            String url = jsonObject.getString("url");
            boolean nsfw = jsonObject.getBoolean("nsfw");
            recentRelease = jsonObject.getString("recent_release");
            series = new MangaUpdatesSeries(seriesId, title, image, url, nsfw);
        }

        if (series.isNsfw() && !slot.getBaseGuildMessageChannel().get().isNSFW()) {
            EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale());
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.sendMessage(false, eb.build(), ActionRow.of(EmbedFactory.getNSFWBlockButton(getLocale())));
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
            slot.sendMessage(true, eb.build());
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
                            String chapter = StringUtil.escapeMarkdown(release.getChapter().substring(series.getTitle().length()).trim());
                            String scanlator = StringUtil.escapeMarkdown(release.getScanlator());
                            if (!chapter.isBlank()) {
                                return getString("release", chapter, scanlator);
                            } else {
                                return getString("release_nochapter", scanlator);
                            }
                        }
                );

        if (releases.size() > maxSlots) {
            recentReleases += getString("viewmore", series.getReleasesUrl());
        }

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
