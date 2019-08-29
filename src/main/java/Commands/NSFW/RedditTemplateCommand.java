package Commands.NSFW;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import General.Reddit.RedditDownloader;
import General.Reddit.RedditPost;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

public class RedditTemplateCommand extends Command implements onRecievedListener {
    private String subreddit;

    public RedditTemplateCommand(String subreddit) {
        super();
        this.subreddit = subreddit;
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = true;
        withLoadingBar = true;
        emoji = "\uD83D\uDD1E";
        executable = true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        RedditPost post;

        int tries = 5;
        do {
            post = RedditDownloader.getImagePost(locale, subreddit);
            tries--;
        }
        while (post == null && tries >= 0);

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, post.getDescription())
                .setTitle(post.getTitle())
                .setImage(post.getImage())
                .setUrl(post.getLink())
                .setTimestamp(post.getInstant());

        eb.setFooter(TextManager.getString(locale, TextManager.COMMANDS,"porn_footer", Tools.numToString(locale, post.getScore()), Tools.numToString(locale, post.getComments())));

        event.getChannel().sendMessage(eb).get();
        return true;
    }
}
