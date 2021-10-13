package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
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
import net.dv8tion.jda.api.entities.TextChannel;

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
    public boolean onTrigger(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        if (forceSubreddit == null) {
            args = args.trim();
            if (args.startsWith("r/")) {
                args = args.substring(2);
            }
        } else {
            args = forceSubreddit;
        }

        if (args.length() == 0) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        } else {
            RedditPost post;
            int errors = 0;
            do {
                post = redditDownloader.getPost(getLocale(), args);
            } while (++errors < 5 && post != null && (post.isNsfw() && !event.getChannel().isNSFW()));

            if (post != null) {
                if (post.isNsfw() && !event.getChannel().isNSFW()) {
                    setActionRows(ActionRows.of(EmbedFactory.getNSFWBlockButton(getLocale())));
                    drawMessageNew(EmbedFactory.getNSFWBlockEmbed(getLocale())).exceptionally(ExceptionLogger.get());
                    return false;
                }

                EmbedBuilder eb = getEmbed(post);
                EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
                drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                return true;
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getNoResultsString(getLocale(), args));
                drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                return false;
            }
        }
    }

    private EmbedBuilder getEmbed(RedditPost post) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, StringUtil.shortenString(post.getDescription(), 5000))
                .setTitle(post.getTitle())
                .setAuthor(post.getAuthor(), "https://www.reddit.com/user/" + post.getAuthor(), null)
                .setTimestamp(post.getInstant());

        if (InternetUtil.stringIsURL(post.getThumbnail())) {
            eb.setThumbnail(post.getThumbnail());
        }
        if (InternetUtil.stringIsURL(post.getUrl())) {
            eb.setTitle(post.getTitle(), post.getUrl());
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
    public AlertResponse onTrackerRequest(TrackerData slot) throws Throwable {
        String key = forceSubreddit != null ? forceSubreddit : slot.getCommandKey();
        if (key.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.getTextChannel().get().sendMessageEmbeds(eb.build()).complete();
            return AlertResponse.STOP_AND_DELETE;
        } else {
            slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
            PostBundle<RedditPost> postBundle = redditDownloader.getPostTracker(getLocale(), key, slot.getArgs().orElse(null));
            TextChannel channel = slot.getTextChannel().get();
            boolean containsOnlyNsfw = true;

            if (postBundle != null) {
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
                    channel.sendMessageEmbeds(eb.build())
                            .setActionRows(ActionRows.of(EmbedFactory.getNSFWBlockButton(getLocale())))
                            .complete();
                    return AlertResponse.STOP_AND_DELETE;
                }

                if (embedList.size() > 0) {
                    slot.sendMessage(true, embedList);
                }

                slot.setArgs(postBundle.getNewestPost());
                return AlertResponse.CONTINUE_AND_SAVE;
            } else {
                if (slot.getArgs().isEmpty()) {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                            .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                            .setDescription(TextManager.getNoResultsString(getLocale(), key));
                    EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                    channel.sendMessageEmbeds(eb.build()).complete();
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
