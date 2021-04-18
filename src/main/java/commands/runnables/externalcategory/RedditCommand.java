package commands.runnables.externalcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import modules.PostBundle;
import modules.reddit.RedditDownloader;
import modules.reddit.RedditPost;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "reddit",
        emoji = "\uD83E\uDD16",
        executableWithoutArgs = false
)
public class RedditCommand extends Command implements OnAlertListener {

    public RedditCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        args = args.trim();
        if (args.startsWith("r/")) args = args.substring(2);

        if (args.length() == 0) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args")).build()).queue();
            return false;
        } else {
            addLoadingReactionInstantly();
            RedditPost post;
            post = RedditDownloader.getPost(getLocale(), args);

            if (post != null) {
                if (post.isNsfw() && !event.getChannel().isNSFW()) {
                    event.getChannel().sendMessage(EmbedFactory.getNSFWBlockEmbed(getLocale()).build()).queue();
                    return false;
                }

                EmbedBuilder eb = getEmbed(post);
                EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
                event.getChannel().sendMessage(eb.build()).queue();
                return true;
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                        .setDescription(TextManager.getNoResultsString(getLocale(), args));
                event.getChannel().sendMessage(eb.build()).queue();
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
            nsfwString = " " + getString("nsfw");
        }

        EmbedUtil.setFooter(eb, this, getString("footer", flairText, StringUtil.numToString(post.getScore()), StringUtil.numToString(post.getComments()), post.getDomain()) + nsfwString);

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerData slot) throws Throwable {
        if (slot.getCommandKey().isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.getTextChannel().get().sendMessage(eb.build()).complete();
            return TrackerResult.STOP_AND_DELETE;
        } else {
            slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
            PostBundle<RedditPost> postBundle = RedditDownloader.getPostTracker(getLocale(), slot.getCommandKey(), slot.getArgs().orElse(null));
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
                    channel.sendMessage(eb.build()).complete();
                    return TrackerResult.STOP_AND_DELETE;
                }

                if (embedList.size() > 0) {
                    MessageEmbed[] embedArray = embedList.toArray(new MessageEmbed[0]);
                    slot.sendMessage(true, embedArray);
                }

                slot.setArgs(postBundle.getNewestPost());
                return TrackerResult.CONTINUE_AND_SAVE;
            } else {
                if (slot.getArgs().isEmpty()) {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                            .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                            .setDescription(TextManager.getNoResultsString(getLocale(), slot.getCommandKey()));
                    EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                    channel.sendMessage(eb.build()).complete();
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
