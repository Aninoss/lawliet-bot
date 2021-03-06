package mysql.modules.giveaway;

import core.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Observable;
import java.util.Optional;

public class GiveawayBean extends Observable {

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

    public GiveawayBean(long guildId, long channelId, long messageId, String emoji, int winners, Instant start, long durationMinutes, String title, String description, String imageUrl, boolean active) {
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
        this.active = active || getEnd().isAfter(Instant.now());
    }

    public long getGuildId() {
        return guildId;
    }

    public Optional<Guild> getGuild() {
        return ShardManager.getInstance().getLocalGuildById(guildId);
    }

    public long getMessageId() {
        return messageId;
    }

    public Optional<TextChannel> getChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(channelId));
    }

    public long getChannelId() {
        return channelId;
    }

    public Optional<RestAction<Message>> retrieveMessage() {
        return getChannel().map(channel -> channel.retrieveMessageById(messageId));
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
