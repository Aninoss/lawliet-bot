package modules.suggestions;

import constants.Emojis;
import core.assets.GuildAsset;
import core.utils.JDAEmojiUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class SuggestionMessage implements GuildAsset {

    private final long guildId;
    private final long messageId;
    private final String content;
    private final String author;
    private int upvotes = 0;
    private int downvotes = 0;
    private boolean loaded = false;

    public SuggestionMessage(long guildId, long messageId, String content, String author) {
        this.guildId = guildId;
        this.messageId = messageId;
        this.content = content;
        this.author = author;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    public long getMessageId() {
        return messageId;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public synchronized int getUpvotes() {
        return upvotes;
    }

    public synchronized int getDownvotes() {
        return downvotes;
    }

    public synchronized void updateUpvotes(int offset) {
        upvotes += offset;
    }

    public synchronized void updateDownvotes(int offset) {
        downvotes += offset;
    }

    public void loadVoteValuesifAbsent(TextChannel channel) {
        if (!loaded) {
            loaded = true;
            Message message = channel.retrieveMessageById(messageId).complete();
            upvotes = JDAEmojiUtil.getMessageReactionFromMessage(message, Emojis.LIKE).map(r -> r.getCount() - 1).orElse(0);
            downvotes = JDAEmojiUtil.getMessageReactionFromMessage(message, Emojis.DISLIKE).map(r -> r.getCount() - 1).orElse(0);
        }
    }

}
