package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.components.ActionRows;
import core.utils.EmbedUtil;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import modules.PostBundle;
import modules.reddit.RedditDownloader;
import modules.reddit.RedditPost;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.StandardGuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "reddit",
        emoji = "\uD83E\uDD16",
        executableWithoutArgs = false
)
public class RedditCommand extends Command implements OnAlertListener {

    private static final RedditDownloader redditDownloader = new RedditDownloader();

    private final String forceSubreddit;

    public RedditCommand(Locale locale, String prefix) {
        this(locale, prefix, null);
    }

    public RedditCommand(Locale locale, String prefix, String forceSubreddit) {
        super(locale, prefix);
        this.forceSubreddit = forceSubreddit;
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException, JsonProcessingException {
        if (forceSubreddit != null) {
            args = forceSubreddit;
        }

        if (args.length() == 0) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        } else {
            String finalArgs = args;
            event.deferReply();
            return redditDownloader.retrievePost(event.getGuild().getIdLong(), args, event.getTextChannel().isNSFW()).get()
                    .map(post -> {
                        if (post.isNsfw() && !event.getTextChannel().isNSFW()) {
                            setActionRows(ActionRows.of(EmbedFactory.getNSFWBlockButton(getLocale())));
                            drawMessageNew(EmbedFactory.getNSFWBlockEmbed(this)).exceptionally(ExceptionLogger.get());
                            return false;
                        }

                        EmbedBuilder eb = getEmbed(post);
                        EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
                        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                        return true;
                    }).orElseGet(() -> {
                        EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, finalArgs);
                        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                        return false;
                    });
        }
    }

    private EmbedBuilder getEmbed(RedditPost post) {
        String desc = post.getDescription();
        if (post.getSourceLink() != null) {
            String add = TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_linktext", post.getSourceLink());
            desc = StringUtil.shortenString(desc, 2048 - add.length());
            desc += add;
        } else {
            desc = StringUtil.shortenString(desc, 2048);
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, desc)
                .setTitle(StringUtil.shortenString(post.getTitle(), 256))
                .setAuthor(post.getAuthor(), "https://www.reddit.com/user/" + post.getAuthor(), null)
                .setTimestamp(post.getInstant());

        if (InternetUtil.stringIsURL(post.getThumbnail())) {
            eb.setThumbnail(post.getThumbnail());
        }
        if (InternetUtil.stringIsURL(post.getUrl())) {
            eb.setTitle(StringUtil.shortenString(post.getTitle(), 256), post.getUrl());
        }
        if (InternetUtil.stringIsURL(post.getImage())) {
            eb.setImage(post.getImage());
        }

        String flairText = "";
        String flair = post.getFlair();
        if (flair != null && !("" + flair).equals("null") && !("" + flair).equals("") && !("" + flair).equals(" ")) {
            flairText = flair + " | ";
        }

        String nsfwString = "";
        if (post.isNsfw()) {
            nsfwString = " " + TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_nsfw");
        }

        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_footer", flairText, StringUtil.numToString(post.getScore()), StringUtil.numToString(post.getComments()), post.getDomain()) + nsfwString);
        return eb;
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        String key = forceSubreddit != null ? forceSubreddit : slot.getCommandKey();
        if (key.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.sendMessage(false, eb.build());
            return AlertResponse.STOP_AND_DELETE;
        } else {
            slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
            Optional<PostBundle<RedditPost>> postBundleOpt = redditDownloader.retrievePostsBulk(key, slot.getArgs().orElse(null)).get();
            StandardGuildMessageChannel channel = slot.getStandardGuildMessageChannel().get();
            boolean containsOnlyNsfw = true;

            if (postBundleOpt.isPresent()) {
                PostBundle<RedditPost> postBundle = postBundleOpt.get();
                ArrayList<MessageEmbed> embedList = new ArrayList<>();
                for (int i = 0; i < Math.min(5, postBundle.getPosts().size()); i++) {
                    RedditPost post = postBundle.getPosts().get(i);
                    if (!post.isNsfw() || channel.isNSFW()) {
                        embedList.add(getEmbed(post).build());
                        containsOnlyNsfw = false;
                        if (slot.getArgs().isEmpty()) {
                            break;
                        }
                    }
                }

                if (containsOnlyNsfw && slot.getArgs().isEmpty()) {
                    EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale());
                    EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                    slot.sendMessage(false, eb.build(), ActionRow.of(EmbedFactory.getNSFWBlockButton(getLocale())));
                    return AlertResponse.STOP_AND_DELETE;
                }

                if (embedList.size() > 0) {
                    slot.sendMessage(true, embedList);
                }

                slot.setArgs(postBundle.getNewestPost());
                return AlertResponse.CONTINUE_AND_SAVE;
            } else {
                if (slot.getArgs().isEmpty()) {
                    EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, key);
                    EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                    slot.sendMessage(false, eb.build());
                    return AlertResponse.STOP_AND_DELETE;
                } else {
                    return AlertResponse.CONTINUE;
                }
            }
        }
    }

    @Override
    public boolean trackerUsesKey() {
        return forceSubreddit == null;
    }

}
