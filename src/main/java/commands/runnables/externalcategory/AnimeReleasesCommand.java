package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.utils.EmbedUtil;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import modules.PostBundle;
import modules.animerelease.AnimeReleasePost;
import modules.animerelease.AnimeReleasesDownloader;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "crunchyroll",
        emoji = "\uD83D\uDCFA",
        executableWithoutArgs = true,
        aliases = { "animereleases", "animerelease" }
)
public class AnimeReleasesCommand extends Command implements OnAlertListener {

    public AnimeReleasesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        event.deferReply();
        PostBundle<AnimeReleasePost> posts = AnimeReleasesDownloader.getPosts(getLocale(), null, args);

        if (posts.getPosts().size() > 0) {
            EmbedBuilder eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), getEmbed(posts.getPosts().get(0)), getPrefix(), getTrigger());
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .setDescription(getString("no_results", false, args));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    private EmbedBuilder getEmbed(AnimeReleasePost post) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setAuthor(post.getAnime(), post.getUrl(), "https://cdn.discordapp.com/attachments/499629904380297226/782242962201116723/crunchyroll_favicon.png")
                .setDescription(post.getDescription())
                .setTimestamp(post.getInstant());
        EmbedUtil.setFooter(eb, this);

        if (post.getEpisode().isPresent()) {
            if (post.getEpisodeTitle().isPresent()) {
                eb.setTitle(getString("template_title", post.getEpisode().get(), post.getEpisodeTitle().get()), post.getUrl());
            } else {
                eb.setTitle(getString("template_title_bundle", post.getEpisode().get()), post.getUrl());
            }
        } else {
            if (post.getEpisodeTitle().isPresent()) {
                eb.setTitle(post.getEpisodeTitle().get(), post.getUrl());
            }
        }

        if (InternetUtil.stringIsURL(post.getThumbnail())) {
            eb.setImage(post.getThumbnail());
        }

        return eb;
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        boolean first = slot.getArgs().isEmpty();
        PostBundle<AnimeReleasePost> postBundle = AnimeReleasesDownloader.getPosts(getLocale(), slot.getArgs().orElse(null), slot.getCommandKey());

        ArrayList<MessageEmbed> embedList = postBundle.getPosts().stream()
                .limit(5)
                .map(post -> getEmbed(post).build())
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.reverse(embedList);
        if (embedList.size() > 0) {
            slot.sendMessage(true, embedList);
        }

        if (first && postBundle.getPosts().size() == 0) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .setDescription(getString("no_results", true, StringUtil.shortenString(slot.getCommandKey(), 200)));
            slot.sendMessage(false, eb.build());
        }

        if (postBundle.getNewestPost() != null) {
            slot.setArgs(postBundle.getNewestPost());
        }
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}