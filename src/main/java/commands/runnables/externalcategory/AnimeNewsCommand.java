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
import modules.PostBundle;
import modules.animenews.AnimeNewsDownloader;
import modules.animenews.AnimeNewsPost;
import mysql.modules.tracker.TrackerSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        AnimeNewsPost post = AnimeNewsDownloader.getPost(getLocale());
        EmbedBuilder eb;
        if (post != null) {
            eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), getEmbed(post), getPrefix(), getTrigger());
        } else {
            eb = EmbedFactory.getApiDownEmbed(getLocale(), getPrefix() + getTrigger());
        }
        event.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

    private EmbedBuilder getEmbed(AnimeNewsPost post) {
        return EmbedFactory.getEmbedDefault(this, post.getDescription())
                .setAuthor(post.getAuthor())
                .setTitle(post.getTitle(), post.getLink())
                .setImage(post.getImage())
                .setTimestamp(post.getInstant());
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerSlot slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
        PostBundle<AnimeNewsPost> postBundle = AnimeNewsDownloader.getPostTracker(getLocale(), slot.getArgs().orElse(null));

        if (postBundle == null) {
            return TrackerResult.CONTINUE;
        }

        TextChannel channel = slot.getTextChannel().get();
        for (AnimeNewsPost post : postBundle.getPosts()) {
            channel.sendMessage(getEmbed(post).build()).complete();
        }

        slot.setArgs(postBundle.getNewestPost());
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}