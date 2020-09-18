package commands.commandrunnables.externalcategory;

import commands.commandlisteners.CommandProperties;

import commands.commandlisteners.OnTrackerRequestListener;
import commands.Command;
import constants.TrackerResult;
import core.*;
import modules.animenews.AnimeNewsDownloader;
import modules.animenews.AnimeNewsPost;
import modules.PostBundle;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@CommandProperties(
    trigger = "animenews",
    withLoadingBar = true,
    emoji = "\uD83D\uDCF0",
    executable = true
)
public class AnimeNewsCommand extends Command implements OnTrackerRequestListener {

    public AnimeNewsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

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