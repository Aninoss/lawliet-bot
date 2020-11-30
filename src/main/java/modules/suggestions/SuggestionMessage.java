package modules.suggestions;

public class SuggestionMessage {

    private final long serverId;
    private final long messageId;
    private final String content;
    private final String author;

    public SuggestionMessage(long serverId, long messageId, String content, String author) {
        this.serverId = serverId;
        this.messageId = messageId;
        this.content = content;
        this.author = author;
    }

    public long getServerId() {
        return serverId;
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
