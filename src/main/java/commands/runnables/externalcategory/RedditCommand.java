package commands.runnables.externalcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import modules.PostBundle;
import modules.reddit.RedditDownloader;
import modules.reddit.RedditPost;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@CommandProperties(
    trigger = "reddit",
    withLoadingBar = true,
    emoji = "\uD83E\uDD16",
    executableWithoutArgs = false
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
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"))).get();
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
                EmbedUtil.addTrackerNoteLog(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), eb, getPrefix(), getTrigger());
                event.getChannel().sendMessage(eb).get();
                return true;
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getNoResultsString(getLocale(), followedString));
                event.getChannel().sendMessage(eb).get();
                return false;
            }
        }
    }

    private EmbedBuilder getEmbed(RedditPost post) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, post.getDescription())
                .setTitle(post.getTitle())
                .setAuthor(post.getAuthor(), "https://www.reddit.com/user/" + post.getAuthor(), "")
                .setTimestamp(post.getInstant());

        if (InternetUtil.stringHasURL(post.getThumbnail(), true))
            eb.setThumbnail(post.getThumbnail());
        if (InternetUtil.stringHasURL(post.getUrl(), true))
            eb.setUrl(post.getUrl());
        if (InternetUtil.stringHasURL(post.getImage(), true))
            eb.setImage(post.getImage());

        String flairText = "";
        String flair = post.getFlair();
        if (flair != null && !("" + flair).equals("null") && !("" + flair).equals("") && !("" + flair).equals(" "))
            flairText = flair + " | ";

        String nsfwString = "";
        if (post.isNsfw()) {
            nsfwString = " " + getString("nsfw");
        }

        EmbedUtil.setFooter(eb, this, getString("footer", flairText, StringUtil.numToString(post.getScore()), StringUtil.numToString(post.getComments()), post.getDomain()) + nsfwString);

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        if (slot.getCommandKey().isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.getChannel().get().sendMessage(eb).get();
            return TrackerResult.STOP_AND_DELETE;
        } else {
            slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
            PostBundle<RedditPost> postBundle = RedditDownloader.getPostTracker(getLocale(), slot.getCommandKey(), slot.getArgs().orElse(null));
            ServerTextChannel channel = slot.getChannel().get();
            boolean containsOnlyNsfw = true;

            if (postBundle != null) {
                for(int i = 0; i < Math.min(5, postBundle.getPosts().size()); i++) {
                    RedditPost post = postBundle.getPosts().get(i);
                    if (!post.isNsfw() || channel.isNsfw()) {
                        channel.sendMessage(getEmbed(post)).get();
                        containsOnlyNsfw = false;
                        if (slot.getArgs().isEmpty())
                            break;
                    }
                }

                if (containsOnlyNsfw && slot.getArgs().isEmpty()) {
                    EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale());
                    EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                    channel.sendMessage(eb).get();
                    return TrackerResult.STOP_AND_DELETE;
                }

                slot.setArgs(postBundle.getNewestPost());
                return TrackerResult.CONTINUE_AND_SAVE;
            } else {
                if (slot.getArgs().isEmpty()) {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                            .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                            .setDescription(TextManager.getNoResultsString(getLocale(), slot.getCommandKey()));
                    EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                    channel.sendMessage(eb).get();
                    return TrackerResult.STOP_AND_DELETE;
                } else {
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
