package Commands.ExternalCategory;

import CommandListeners.CommandProperties;

import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.TrackerResult;
import Modules.AnimeNews.AnimeReleaseDownloader;
import Modules.AnimeNews.AnimeReleasePost;
import Core.EmbedFactory;
import Core.ExceptionHandler;
import Modules.PostBundle;
import Core.TextManager;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@CommandProperties(
    trigger = "animereleases",
    withLoadingBar = true,
    emoji = "\uD83D\uDCFA",
    executable = true
)
public class AnimeReleasesCommand extends Command implements OnTrackerRequestListener {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        PostBundle<AnimeReleasePost> posts = AnimeReleaseDownloader.getPosts(getLocale(), null, followedString);

        if (posts.getPosts().size() > 0) {
            EmbedBuilder eb = EmbedFactory.addLog(getEmbed(posts.getPosts().get(0)), LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "tracker", getPrefix(), getTrigger()));
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

    private EmbedBuilder getEmbed(AnimeReleasePost post) throws IOException {
        if (post == null) {
            ExceptionHandler.showErrorLog("Anime Release Post is null");
        }
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
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(30, ChronoUnit.MINUTES));
        boolean first = !slot.getArgs().isPresent();
        PostBundle<AnimeReleasePost> postBundle = AnimeReleaseDownloader.getPosts(getLocale(), slot.getArgs().orElse(null), slot.getCommandKey().get());

        ServerTextChannel channel = slot.getChannel().get();
        for(int i = postBundle.getPosts().size() - 1; i >= Math.max(0, postBundle.getPosts().size() - 10); i--) {
            AnimeReleasePost post = postBundle.getPosts().get(i);
            channel.sendMessage(getEmbed(post)).get();
        }

        if (first && postBundle.getPosts().size() == 0) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(getString("no_results", slot.getCommandKey().get()));
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