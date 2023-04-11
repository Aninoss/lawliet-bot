package modules.suggestions;

import constants.Emojis;
import core.assets.GuildAsset;
import core.utils.EmojiUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class SuggestionMessage implements GuildAsset {

    private final long guildId;
    private final long messageId;
    private final Long userId;
    private final String content;
    private final String author;
    private int upvotes = 0;
    private int downvotes = 0;
    private boolean loaded = false;

    public SuggestionMessage(long guildId, long messageId, Long userId, String content, String author, int upvotes, int downvotes) {
        this(guildId, messageId, userId, content, author);
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        this.loaded = true;
    }

    public SuggestionMessage(long guildId, long messageId, long userId, String content, String author) {
        this.guildId = guildId;
        this.messageId = messageId;
        this.userId = userId != 0 ? userId : null;
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

    public Long getUserId() {
        return userId;
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
            upvotes = EmojiUtil.getMessageReactionFromMessage(message, Emojis.LIKE).map(r -> r.getCount() - 1).orElse(0);
            downvotes = EmojiUtil.getMessageReactionFromMessage(message, Emojis.DISLIKE).map(r -> r.getCount() - 1).orElse(0);
        }
    }

}
