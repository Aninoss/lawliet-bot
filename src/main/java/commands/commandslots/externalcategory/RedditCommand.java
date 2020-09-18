package commands.commandslots.externalcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandlisteners.OnTrackerRequestListener;
import commands.Command;
import constants.Category;
import constants.TrackerResult;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import modules.PostBundle;
import modules.reddit.RedditDownloader;
import modules.reddit.RedditPost;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@CommandProperties(
    trigger = "reddit",
    withLoadingBar = true,
    emoji = "\uD83E\uDD16",
    executable = false
)
public class RedditCommand extends Command implements OnTrackerRequestListener {

    public RedditCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        followedString = StringUtil.trimString(followedString);
        if (followedString.startsWith("r/")) followedString = followedString.substring(2);

        if (followedString.length() == 0) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"))).get();
            return false;
        } else {
            RedditPost post;
            post = RedditDownloader.getPost(getLocale(), followedString);

            if (post != null) {
                if (post.isNsfw() && !event.getServerTextChannel().get().isNsfw()) {
                    event.getChannel().sendMessage(EmbedFactory.getNSFWBlockEmbed(getLocale())).get();
                    return false;
                }

                EmbedBuilder eb = getEmbed(post);
                EmbedFactory.addTrackerNote(getLocale(), eb, getPrefix(), getTrigger());
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
    }

    private EmbedBuilder getEmbed(RedditPost post) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, post.getDescription())
                .setTitle(post.getTitle())
                .setThumbnail(post.getThumbnail())
                .setAuthor(post.getAuthor(), "https://www.reddit.com/user/" + post.getAuthor(), "")
                .setTimestamp(post.getInstant())
                .setImage(post.getImage())
                .setUrl(post.getLink());

        String flairText = "";
        String flair = post.getFlair();
        if (flair != null && !("" + flair).equals("null") && !("" + flair).equals("") && !("" + flair).equals(" "))
            flairText = flair + " | ";

        String nsfwString = "";
        if (post.isNsfw()) {
            nsfwString = " " + getString("nsfw");
        }

        eb.setFooter(getString("footer", flairText, StringUtil.numToString(getLocale(), post.getScore()), StringUtil.numToString(getLocale(), post.getComments()), post.getDomain()) + nsfwString);

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        ServerTextChannel channel = slot.getChannel().get();
        if (!slot.getCommandKey().isPresent() || slot.getCommandKey().get().length() == 0) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
            EmbedFactory.addTrackerRemoveLog(eb, getLocale());
            channel.sendMessage(eb).get();
            return TrackerResult.STOP_AND_DELETE;
        } else {
            slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
            PostBundle<RedditPost> postBundle = RedditDownloader.getPostTracker(getLocale(), slot.getCommandKey().get(), slot.getArgs().orElse(null));

            boolean containsOnlyNsfw = true;

            if (postBundle != null) {
                for(int i = 0; i < Math.min(5, postBundle.getPosts().size()); i++) {
                    RedditPost post = postBundle.getPosts().get(i);
                    if (!post.isNsfw() || channel.isNsfw()) {
                        if (slot.getArgs().isPresent() || i == 0)
                            channel.sendMessage(getEmbed(post));
                        containsOnlyNsfw = false;
                    }
                }

                if (containsOnlyNsfw && !slot.getArgs().isPresent()) {
                    EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale());
                    EmbedFactory.addTrackerRemoveLog(eb, getLocale());
                    channel.sendMessage(eb).get();
                    return TrackerResult.STOP_AND_DELETE;
                }

                slot.setArgs(postBundle.getNewestPost());
                return TrackerResult.CONTINUE_AND_SAVE;
            } else {
                if (!slot.getArgs().isPresent()) {
                    EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                            .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                            .setDescription(TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_noresults_tracker", slot.getCommandKey().get()));
                    EmbedFactory.addTrackerRemoveLog(eb, getLocale());
                    channel.sendMessage(eb).get();
                    return TrackerResult.STOP_AND_DELETE;
                } else {
                    if (RedditDownloader.checkRedditConnection()) {
                        slot.setArgs(null);
                        return TrackerResult.CONTINUE_AND_SAVE;
                    }

                    return TrackerResult.CONTINUE;
                }
            }
        }
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}
