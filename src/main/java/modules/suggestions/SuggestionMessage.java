package modules.suggestions;

import core.assets.GuildAsset;

public class SuggestionMessage implements GuildAsset {

    private final long guildId;
    private final long messageId;
    private final String content;
    private final String author;

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

}
