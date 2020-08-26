package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import Constants.TrackerResult;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Utils.StringUtil;
import Modules.AnimeRelease.AnimeReleaseDownloader;
import Modules.AnimeRelease.AnimeReleasePost;
import Modules.PostBundle;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@CommandProperties(
        trigger = "animereleases",
        withLoadingBar = true,
        emoji = "\uD83D\uDCFA",
        executable = true,
        aliases = { "animerelease" }
)
public class AnimeReleasesCommand extends Command implements OnTrackerRequestListener {

    public AnimeReleasesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        PostBundle<AnimeReleasePost> posts = AnimeReleaseDownloader.getPosts(getLocale(), null, followedString);

        if (posts.getPosts().size() > 0) {
            EmbedBuilder eb = EmbedFactory.addTrackerNote(getLocale(), getEmbed(posts.getPosts().get(0)), getPrefix(), getTrigger());
            event.getChannel().sendMessage(eb).get();
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
    }

    private EmbedBuilder getEmbed(AnimeReleasePost post) {
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setAuthor(post.getAnime(), post.getUrl(), "https://www.crunchyroll.com/favicons/favicon-32x32.png")
                .setDescription(post.getDescription())
                .setUrl(post.getUrl())
                .setImage(post.getThumbnail())
                .setTimestamp(post.getDate());

        if (post.getEpisode().isPresent()) {
            if (post.getEpisodeTitle().isPresent()) eb.setTitle(getString("template_title",post.getEpisode().get(), post.getEpisodeTitle().get()));
            else eb.setTitle(getString("template_title_bundle", post.getEpisode().get()));
        } else {
            if (post.getEpisodeTitle().isPresent()) eb.setTitle(post.getEpisodeTitle().get());
        }

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        boolean first = !slot.getArgs().isPresent();
        PostBundle<AnimeReleasePost> postBundle = AnimeReleaseDownloader.getPosts(getLocale(), slot.getArgs().orElse(null), slot.getCommandKey().get());

        ServerTextChannel channel = slot.getChannel().get();
        for(int i = Math.min(9, postBundle.getPosts().size() - 1); i >= 0; i--) {
            AnimeReleasePost post = postBundle.getPosts().get(i);
            channel.sendMessage(getEmbed(post)).get();
        }

        if (first && postBundle.getPosts().size() == 0) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(getString("no_results", StringUtil.shortenString(slot.getCommandKey().get(), 200)));
            slot.getChannel().get().sendMessage(eb).get();
        }

        if (postBundle.getNewestPost() != null) slot.setArgs(postBundle.getNewestPost());
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}