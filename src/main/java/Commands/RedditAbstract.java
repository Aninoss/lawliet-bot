package Commands;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import General.Reddit.RedditDownloader;
import General.Reddit.RedditPost;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

public abstract class RedditAbstract extends Command implements onRecievedListener {

    public abstract String getSubreddit();

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
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

        eb.setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS,"porn_footer", Tools.numToString(getLocale(), post.getScore()), Tools.numToString(getLocale(), post.getComments())));

        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
