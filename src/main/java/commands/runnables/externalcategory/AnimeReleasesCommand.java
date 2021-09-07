package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import modules.schedulers.AlertResponse;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import modules.PostBundle;
import modules.animerelease.AnimeReleasePost;
import modules.animerelease.AnimeReleasesDownloader;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        PostBundle<AnimeReleasePost> posts = AnimeReleasesDownloader.getPosts(getLocale(), null, args);

        if (posts.getPosts().size() > 0) {
            EmbedBuilder eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), getEmbed(posts.getPosts().get(0)), getPrefix(), getTrigger());
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .setDescription(getString("no_results", false, args));
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
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
    public AlertResponse onTrackerRequest(TrackerData slot) throws Throwable {
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
            slot.getTextChannel().get().sendMessageEmbeds(eb.build()).complete();
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