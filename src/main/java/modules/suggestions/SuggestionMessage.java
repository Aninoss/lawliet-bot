package modules.suggestions;

public class SuggestionMessage {

    private final long serverId;
    private final long messageId;
    private final String content;

    public SuggestionMessage(long serverId, long messageId, String content) {
        this.serverId = serverId;
        this.messageId = messageId;
        this.content = content;
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

}
