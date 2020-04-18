package Commands;


import CommandSupporters.Command;
import Core.*;
import Modules.Reddit.RedditDownloader;
import Modules.Reddit.RedditPost;
import Core.Tools.StringTools;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

public abstract class RedditAbstract extends Command {

    public abstract String getSubreddit();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        RedditPost post;

        int tries = 5;
        do {
            post = RedditDownloader.getImagePost(getLocale(), getSubreddit());
            tries--;
        }
        while (post == null && tries >= 0);

        if (post == null) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "error"))
                    .setDescription(TextManager.getString(getLocale(), TextManager.COMMANDS, "reddit_error", followedString));

            event.getChannel().sendMessage(eb).get();

            return false;
        }

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, post.getDescription())
                .setTitle(post.getTitle())
                .setImage(post.getImage())
                .setUrl(post.getLink())
                .setTimestamp(post.getInstant());

        eb.setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS,"post_footer", StringTools.numToString(getLocale(), post.getScore()), StringTools.numToString(getLocale(), post.getComments())));

        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
