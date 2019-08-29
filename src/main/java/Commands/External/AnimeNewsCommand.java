package Commands.External;

import CommandListeners.onRecievedListener;
import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import General.*;
import General.AnimeNews.AnimeNewsDownloader;
import General.AnimeNews.AnimeNewsPost;
import General.PostBundle;
import General.Tracker.TrackerData;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;

public class AnimeNewsCommand extends Command implements onRecievedListener, onTrackerRequestListener {
    public AnimeNewsCommand() {
        super();
        trigger = "animenews";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = true;
        emoji = "\uD83D\uDCF0";
        executable = true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        AnimeNewsPost post = AnimeNewsDownloader.getPost(locale);
        event.getChannel().sendMessage(getEmbed(post)).get();
        return true;
    }

    private EmbedBuilder getEmbed(AnimeNewsPost post) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, post.getDescription())
                .setAuthor(post.getAuthor())
                .setTitle(post.getTitle())
                .setImage(post.getImage())
                .setUrl(post.getLink())
                .setFooter(getString("footer", Tools.numToString(locale, post.getComments()), post.getDate(), post.getCategory()));

        return eb;
    }

    @Override
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        trackerData.setInstant(Instant.now().plusSeconds(60 * 15));
        PostBundle<AnimeNewsPost> postBundle = AnimeNewsDownloader.getPostTracker(locale, trackerData.getArg());

        for(AnimeNewsPost post: postBundle.getPosts()) {
            trackerData.getChannel().sendMessage(getEmbed(post));
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