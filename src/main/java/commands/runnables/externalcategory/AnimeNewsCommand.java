package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import modules.animenews.AnimeNewsArticle;
import modules.animenews.AnimeNewsDownloader;
import mysql.modules.tracker.TrackerSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        addLoadingReactionInstantly();
        List<AnimeNewsArticle> articles = AnimeNewsDownloader.retrieveArticles(getLocale());
        EmbedBuilder eb;
        if (articles != null && articles.size() > 0) {
            eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), getEmbed(articles.get(0)), getPrefix(), getTrigger());
        } else {
            eb = EmbedFactory.getApiDownEmbed(getLocale(), getPrefix() + getTrigger());
        }
        event.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

    private EmbedBuilder getEmbed(AnimeNewsArticle post) {
        return EmbedFactory.getEmbedDefault(this, post.getDescription())
                .setTitle(post.getTitle(), post.getLink())
                .setImage(post.getThumbnail())
                .setTimestamp(post.getPublicationTime());
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerSlot slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
        List<AnimeNewsArticle> articles = AnimeNewsDownloader.retrieveArticles(getLocale());
        if (articles == null || articles.size() == 0) {
            return TrackerResult.CONTINUE;
        }

        String thresholdString = slot.getArgs().orElse(null);
        MessageEmbed[] embeds;
        if (thresholdString != null) {
            Instant threshold = Instant.parse(thresholdString);
            ArrayList<MessageEmbed> embedList = articles.stream()
                    .filter(article -> article.getPublicationTime().isAfter(threshold))
                    .map(post -> getEmbed(post).build())
                    .collect(Collectors.toCollection(ArrayList::new));

            Collections.reverse(embedList);
            embeds = embedList.toArray(new MessageEmbed[0]);
        } else {
            embeds = new MessageEmbed[] { getEmbed(articles.get(0)).build() };
        }

        slot.sendMessage(embeds);
        slot.setArgs(articles.get(0).getPublicationTime().toString());
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}