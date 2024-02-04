package commands.runnables.externalcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.AssetIds;
import constants.Language;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MainLogger;
import core.utils.EmbedUtil;
import modules.animenews.AnimeNewsArticle;
import modules.animenews.AnimeNewsDownloader;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "animenews",
        emoji = "\uD83D\uDCF0",
        executableWithoutArgs = true
)
public class AnimeNewsCommand extends Command implements OnAlertListener {

    public AnimeNewsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        event.deferReply();
        List<AnimeNewsArticle> articles = AnimeNewsDownloader.retrieveArticles(getLocale());
        EmbedBuilder eb;
        if (articles != null && !articles.isEmpty()) {
            eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), getEmbed(articles.get(0)), getPrefix(), getTrigger());
        } else {
            eb = EmbedFactory.getApiDownEmbed(this, getPrefix() + getTrigger());
        }
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private EmbedBuilder getEmbed(AnimeNewsArticle post) {
        return EmbedFactory.getEmbedDefault(this, post.getDescription())
                .setTitle(post.getTitle(), post.getLink())
                .setImage(post.getThumbnail())
                .setTimestamp(post.getPublicationTime());
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
        Locale locale = slot.getGuildId() == AssetIds.WEBGATE_SERVER_ID
                ? Language.EN.getLocale()
                : getLocale();
        List<AnimeNewsArticle> articles = AnimeNewsDownloader.retrieveArticles(locale);
        if (articles == null || articles.isEmpty()) {
            return AlertResponse.CONTINUE;
        }

        String thresholdString = slot.getArgs().orElse(null);
        List<MessageEmbed> embedList;
        if (thresholdString != null) {
            Instant threshold = Instant.parse(thresholdString);
            embedList = articles.stream()
                    .filter(article -> article.getPublicationTime().isAfter(threshold))
                    .map(post -> getEmbed(post).build())
                    .collect(Collectors.toCollection(ArrayList::new));

            Collections.reverse(embedList);
        } else {
            embedList = List.of(getEmbed(articles.get(0)).build());
        }

        if (!embedList.isEmpty()) {
            slot.sendMessage(getLocale(), true, embedList);
            if (slot.getGuildId() == 1190310706248167506L) {
                MainLogger.get().info("### Anime news triggered");
            }
        }
        slot.setArgs(articles.get(0).getPublicationTime().toString());
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}