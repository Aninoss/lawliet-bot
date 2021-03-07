package commands.runnables;


import commands.Command;
import constants.Category;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import modules.reddit.RedditDownloader;
import modules.reddit.RedditPost;



import java.util.Locale;

public abstract class RedditAbstract extends Command {

    public RedditAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public abstract String getSubreddit();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        RedditPost post;

        int tries = 5;
        do {
            post = RedditDownloader.getImagePost(getLocale(), getSubreddit());
            tries--;
        } while ((post == null || post.isNsfw()) && tries >= 0);

        if (post == null) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "error"))
                    .setDescription(TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_error", followedString));

            event.getChannel().sendMessage(eb).get();

            return false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, post.getDescription())
                .setTitle(post.getTitle())
                .setTimestamp(post.getInstant());

        if (InternetUtil.stringHasURL(post.getUrl(), true))
            eb.setUrl(post.getUrl());
        if (InternetUtil.stringHasURL(post.getImage(), true))
            eb.setImage(post.getImage());

        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.COMMANDS,"post_footer", StringUtil.numToString(post.getScore()), StringUtil.numToString(post.getComments())));

        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
