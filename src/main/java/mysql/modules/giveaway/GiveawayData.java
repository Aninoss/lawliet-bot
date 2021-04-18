package mysql.modules.giveaway;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Observable;
import java.util.Optional;
import core.assets.MessageAsset;

public class GiveawayData extends Observable implements MessageAsset {

    private final long guildId;
    private final long messageId;
    private final long channelId;
    private final String emoji;
    private final int winners;
    private final Instant start;
    private final long durationMinutes;
    private final String title;
    private final String description;
    private final String imageUrl;
    private boolean active;

    public GiveawayData(long guildId, long channelId, long messageId, String emoji, int winners, Instant start, long durationMinutes, String title, String description, String imageUrl, boolean active) {
        this.guildId = guildId;
        this.messageId = messageId;
        this.channelId = channelId;
        this.emoji = emoji;
        this.winners = winners;
        this.start = start;
        this.durationMinutes = durationMinutes;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getTextChannelId() {
        return channelId;
    }

    @Override
    public long getMessageId() {
        return messageId;
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

    public Instant getEnd() {
        return start.plus(durationMinutes, ChronoUnit.MINUTES);
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
        setChanged();
        notifyObservers();
    }

    public boolean isActive() {
        return active;
    }

}
