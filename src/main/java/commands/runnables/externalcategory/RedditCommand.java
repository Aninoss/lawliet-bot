package commands.runnables.externalcategory;

import com.fasterxml.jackson.core.JsonProcessingException;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.InternetUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.reddit.RedditDownloader;
import modules.reddit.RedditPost;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

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

        if (args.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        } else {
            String finalArgs = args;
            deferReply();
            try {
                return redditDownloader.retrievePost(event.getGuild().getIdLong(), args, JDAUtil.channelIsNsfw(event.getChannel())).get()
                        .map(post -> {
                            if (post.isNsfw() && !JDAUtil.channelIsNsfw(event.getChannel())) {
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
            } catch (ExecutionException e) {
                EmbedBuilder eb = EmbedFactory.getApiDownEmbed(this, "reddit.com");
                drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                return false;
            }
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
        Instant nextRequestPreviously = slot.getNextRequest();

        if (key.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.sendMessage(getLocale(), false, eb.build());
            return AlertResponse.STOP_AND_DELETE;
        }

        List<RedditPost> redditPosts;
        try {
            redditPosts = redditDownloader.retrievePostsBulk(key, slot.getArgs().orElse(null)).get();
        } catch (ExecutionException e) {
            slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
            return AlertResponse.CONTINUE;
        }

        slot.setNextRequest(Instant.now().plus(120, ChronoUnit.MINUTES));
        if (redditPosts.isEmpty()) {
            if (slot.getArgs().isEmpty() || nextRequestPreviously.isBefore(Instant.now().minus(Duration.ofDays(30)))) {
                EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, key);
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(getLocale(), false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE_AND_SAVE;
            }
        }

        ArrayList<String> idList = slot.getArgs().map(args -> new ArrayList<>(List.of(args.split("\\|"))))
                .orElse(new ArrayList<>());
        ArrayList<String> newIdList = new ArrayList<>();
        int totalEmbedSize = 0;
        boolean containsOnlyNsfw = true;
        ArrayList<MessageEmbed> embedList = new ArrayList<>();

        if (slot.getArgs().isEmpty()) {
            for (int i = 1; i < Math.min(50, redditPosts.size()); i++) {
                idList.add(0, redditPosts.get(i).getId());
            }
        }
        for (int i = 0; i < Math.min(50, redditPosts.size()) && embedList.size() < 5; i++) {
            RedditPost post = redditPosts.get(i);
            if (post.isNsfw() && !JDAUtil.channelIsNsfw(slot.getGuildMessageChannel().get())) {
                continue;
            }
            containsOnlyNsfw = false;
            if (!idList.contains(post.getId())) {
                MessageEmbed messageEmbed = getEmbed(post).build();
                totalEmbedSize += messageEmbed.getLength();
                embedList.add(0, messageEmbed);
                newIdList.add(0, post.getId());
            }
        }

        idList.addAll(newIdList);
        while (idList.size() > 250) {
            idList.remove(0);
        }

        if (containsOnlyNsfw && slot.getArgs().isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale(), getPrefix());
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.sendMessage(getLocale(), false, eb.build());
            return AlertResponse.STOP_AND_DELETE;
        }

        if (!embedList.isEmpty()) {
            while (totalEmbedSize > MessageEmbed.EMBED_MAX_LENGTH_BOT) {
                totalEmbedSize -= embedList.remove(0).getLength();
            }
            slot.sendMessage(getLocale(), true, embedList);
        }

        slot.setArgs(Strings.join(idList, '|'));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return forceSubreddit == null;
    }

}
