package Commands.External;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import General.*;
import General.AnimeNews.AnimeNewsDownloader;
import General.AnimeNews.AnimeNewsPost;
import General.PostBundle;
import General.Tracker.TrackerData;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import javax.xml.soap.Text;
import java.io.IOException;
import java.time.Instant;

@CommandProperties(
    trigger = "animenews",
    withLoadingBar = true,
    emoji = "\uD83D\uDCF0",
    executable = true
)
public class AnimeNewsCommand extends Command implements onRecievedListener, onTrackerRequestListener {

    public AnimeNewsCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        AnimeNewsPost post = AnimeNewsDownloader.getPost(getLocale());
        EmbedBuilder eb = EmbedFactory.addLog(getEmbed(post), LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "tracker", getPrefix(), getTrigger()));
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed(AnimeNewsPost post) throws IOException {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, post.getDescription())
                .setAuthor(post.getAuthor())
                .setTitle(post.getTitle())
                .setImage(post.getImage())
                .setUrl(post.getLink())
                .setFooter(getString("footer", Tools.numToString(getLocale(), post.getComments()), post.getDate(), post.getCategory()));

        return eb;
    }

    @Override
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        trackerData.setInstant(Instant.now().plusSeconds(60 * 15));
        PostBundle<AnimeNewsPost> postBundle = AnimeNewsDownloader.getPostTracker(getLocale(), trackerData.getArg());

        if (postBundle == null) {
            trackerData.setSaveChanges(false);
            return trackerData;
        }

        for(AnimeNewsPost post: postBundle.getPosts()) {
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