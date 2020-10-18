package mysql.modules.giveaway;

import java.time.Instant;
import java.util.Optional;

public class GiveawayBean {

    private final long serverId;
    private final long messageId;
    private final long channelId;
    private final String emoji;
    private final int winners;
    private final Instant start;
    private final long durationMinutes;
    private final String title;
    private final String description;
    private final String imageUrl;

    private boolean active = true;

    public GiveawayBean(long serverId, long channelId, long messageId, String emoji, int winners, Instant start, long durationMinutes, String title, String description, String imageUrl) {
        this.serverId = serverId;
        this.messageId = messageId;
        this.channelId = channelId;
        this.emoji = emoji;
        this.winners = winners;
        this.start = start;
        this.durationMinutes = durationMinutes;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public long getServerId() {
        return serverId;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getChannelId() {
        return channelId;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getWinners() {
        return winners;
    }

    public Instant getStart() {
        return start;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Optional<String> getImageUrl() {
        return Optional.ofNullable(imageUrl);
    }

    public void stop() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

}
