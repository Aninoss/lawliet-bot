package commands.runnables.externalcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import modules.PostBundle;
import modules.animenews.AnimeNewsDownloader;
import modules.animenews.AnimeNewsPost;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@CommandProperties(
    trigger = "animenews",
    withLoadingBar = true,
    emoji = "\uD83D\uDCF0",
    executableWithoutArgs = true
)
public class AnimeNewsCommand extends Command implements OnTrackerRequestListener {

    public AnimeNewsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        AnimeNewsPost post = AnimeNewsDownloader.getPost(getLocale());
        EmbedBuilder eb;
        if (post != null) eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), getEmbed(post), getPrefix(), getTrigger());
        else eb = EmbedFactory.getApiDownEmbed(getLocale(), getPrefix() + getTrigger());
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed(AnimeNewsPost post) {
        return EmbedFactory.getEmbedDefault(this, post.getDescription())
                .setAuthor(post.getAuthor())
                .setTitle(post.getTitle())
                .setImage(post.getImage())
                .setUrl(post.getLink())
                .setTimestamp(post.getInstant());
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