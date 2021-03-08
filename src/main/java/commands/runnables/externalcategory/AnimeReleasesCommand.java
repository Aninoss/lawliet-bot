package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.PostBundle;
import modules.animerelease.AnimeReleaseDownloader;
import modules.animerelease.AnimeReleasePost;
import mysql.modules.tracker.TrackerBeanSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "crunchyroll",
        withLoadingBar = true,
        emoji = "\uD83D\uDCFA",
        executableWithoutArgs = true,
        aliases = { "animereleases", "animerelease" }
)
public class AnimeReleasesCommand extends Command implements OnTrackerRequestListener {

    public AnimeReleasesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        PostBundle<AnimeReleasePost> posts = AnimeReleaseDownloader.getPosts(getLocale(), null, args);

        if (posts.getPosts().size() > 0) {
            EmbedBuilder eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), getEmbed(posts.getPosts().get(0)), getPrefix(), getTrigger());
            event.getChannel().sendMessage(eb.build()).queue();
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .setDescription(getString("no_results", false, args));
            event.getChannel().sendMessage(eb.build()).queue();
            return false;
        }
    }

    private EmbedBuilder getEmbed(AnimeReleasePost post) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setAuthor(post.getAnime(), post.getUrl(), "https://cdn.discordapp.com/attachments/499629904380297226/782242962201116723/crunchyroll_favicon.png")
                .setDescription(post.getDescription())
                .setImage(post.getThumbnail())
                .setTimestamp(post.getInstant());
        EmbedUtil.setFooter(eb, this);

        if (post.getEpisode().isPresent()) {
            if (post.getEpisodeTitle().isPresent())
                eb.setTitle(getString("template_title",post.getEpisode().get(), post.getEpisodeTitle().get()), post.getUrl());
            else
                eb.setTitle(getString("template_title_bundle", post.getEpisode().get()), post.getUrl());
        } else {
            if (post.getEpisodeTitle().isPresent())
                eb.setTitle(post.getEpisodeTitle().get(), post.getUrl());
        }

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        boolean first = slot.getArgs().isEmpty();
        PostBundle<AnimeReleasePost> postBundle = AnimeReleaseDownloader.getPosts(getLocale(), slot.getArgs().orElse(null), slot.getCommandKey());

        TextChannel channel = slot.getTextChannel().get();
        for(int i = Math.min(4, postBundle.getPosts().size() - 1); i >= 0; i--) {
            AnimeReleasePost post = postBundle.getPosts().get(i);
            channel.sendMessage(getEmbed(post).build()).complete();
        }

        if (first && postBundle.getPosts().size() == 0) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .setDescription(getString("no_results", true, StringUtil.shortenString(slot.getCommandKey(), 200)));
            slot.getTextChannel().get().sendMessage(eb.build()).complete();
        }

        if (postBundle.getNewestPost() != null) slot.setArgs(postBundle.getNewestPost());
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}