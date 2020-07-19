package Commands.ExternalCategory;

import CommandListeners.CommandProperties;

import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import Constants.TrackerResult;
import Core.*;
import Modules.AnimeNews.AnimeNewsDownloader;
import Modules.AnimeNews.AnimeNewsPost;
import Modules.PostBundle;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@CommandProperties(
    trigger = "animenews",
    withLoadingBar = true,
    emoji = "\uD83D\uDCF0",
    executable = true
)
public class AnimeNewsCommand extends Command implements OnTrackerRequestListener {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        AnimeNewsPost post = AnimeNewsDownloader.getPost(getLocale());
        EmbedBuilder eb = EmbedFactory.addTrackerNote(getLocale(), getEmbed(post), getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb).get();

        return true;
    }

    private EmbedBuilder getEmbed(AnimeNewsPost post) throws IOException {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, post.getDescription())
                .setAuthor(post.getAuthor())
                .setTitle(post.getTitle())
                .setImage(post.getImage())
                .setUrl(post.getLink())
                .setTimestamp(post.getInstant());

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
        PostBundle<AnimeNewsPost> postBundle = AnimeNewsDownloader.getPostTracker(getLocale(), slot.getArgs().orElse(null));

        if (postBundle == null)
            return TrackerResult.CONTINUE;

        ServerTextChannel channel = slot.getChannel().get();
        for(AnimeNewsPost post: postBundle.getPosts()) {
            channel.sendMessage(getEmbed(post)).get();
        }

        slot.setArgs(postBundle.getNewestPost());
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}