package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import General.AnimeNews.AnimeReleaseDownloader;
import General.AnimeNews.AnimeReleasePost;
import General.EmbedFactory;
import General.PostBundle;
import General.TextManager;
import General.StringTools;
import General.Tracker.TrackerData;
import org.javacord.api.entity.channel.ServerTextChannel;
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
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        AnimeReleasePost post = AnimeReleaseDownloader.getPost(getLocale());
        EmbedBuilder eb = EmbedFactory.addLog(getEmbed(post), LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "tracker", getPrefix(), getTrigger()));
        event.getChannel().sendMessage(eb).get();
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

        ServerTextChannel channel = trackerData.getChannel().get();
        for(AnimeReleasePost post: postBundle.getPosts()) {
            boolean canPost = trackerData.getKey().equalsIgnoreCase("all");
            if (!canPost) {
                for (String animeName : trackerData.getKey().split(",")) {
                    if (post.getAnime().toLowerCase().contains(StringTools.trimString(animeName.toLowerCase()))) {
                        canPost = true;
                        break;
                    }
                }
            }

            if (canPost) {
                channel.sendMessage(getEmbed(post)).get();
            }
        }

        if (postBundle.getNewestPost() != null) trackerData.setArg(postBundle.getNewestPost());
        return trackerData;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}