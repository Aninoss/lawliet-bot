package Commands.External;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import General.AnimeNews.AnimeReleaseDownloader;
import General.AnimeNews.AnimeReleasePost;
import General.EmbedFactory;
import General.PostBundle;
import General.Tracker.TrackerData;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.io.IOException;
import java.time.Instant;

@CommandProperties(
    trigger = "animereleases",
    withLoadingBar = true,
    emoji = "\uD83D\uDCFA",
    executable = true
)
public class AnimeReleasesCommand extends Command implements onRecievedListener, onTrackerRequestListener {

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        AnimeReleasePost post = AnimeReleaseDownloader.getPost(getLocale());
        event.getChannel().sendMessage(getEmbed(post)).get();
        return true;
    }

    private EmbedBuilder getEmbed(AnimeReleasePost post) throws IOException {
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setAuthor(post.getAnime(), post.getUrl(), "https://www.crunchyroll.com/favicons/favicon-32x32.png")
                .setDescription(post.getDescription())
                .setUrl(post.getUrl())
                .setImage(post.getThumbnail())
                .setTimestamp(post.getDate());

        if (post.getEpisode().isPresent()) {
            eb.setTitle(getString("template_title", post.getEpisode().get(), post.getEpisodeTitle()));
        } else {
            if (!post.getEpisodeTitle().isEmpty()) eb.setTitle(post.getEpisodeTitle());
        }

        return eb;
    }

    @Override
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        trackerData.setInstant(Instant.now().plusSeconds(60 * 5));
        PostBundle<AnimeReleasePost> postBundle = AnimeReleaseDownloader.getPostTracker(getLocale(), trackerData.getArg());

        if (postBundle == null) {
            trackerData.setSaveChanges(false);
            return trackerData;
        }

        for(AnimeReleasePost post: postBundle.getPosts()) {
            trackerData.getChannel().sendMessage(getEmbed(post)).get();
        }

        trackerData.setArg(postBundle.getNewestPost());
        return trackerData;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

    @Override
    public boolean needsPrefix() {
        return false;
    }
}