package mysql.modules.suggestions;

import java.util.HashMap;
import java.util.Optional;
import core.CustomObservableMap;
import modules.suggestions.SuggestionMessage;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.checkerframework.checker.nullness.qual.NonNull;

public class SuggestionsData extends DataWithGuild {

    private final CustomObservableMap<Long, SuggestionMessage> messages;
    private boolean active;
    private Long channelId;

    public SuggestionsData(long serverId, boolean active, Long channelId, @NonNull HashMap<Long, SuggestionMessage> messages) {
        super(serverId);
        this.messages = new CustomObservableMap<>(messages);
        this.active = active;
        this.channelId = channelId;
    }

    public boolean isActive() {
        return active;
    }

    public CustomObservableMap<Long, SuggestionMessage> getSuggestionMessages() {
        return messages;
    }

    public Optional<Long> getTextChannelId() {
        return Optional.ofNullable(channelId);
    }

    public Optional<TextChannel> getTextChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(channelId != null ? channelId : 0L));
    }

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

    public void setChannelId(Long channelId) {
        if (this.channelId == null || !this.channelId.equals(channelId)) {
            this.channelId = channelId;
            setChanged();
            notifyObservers();
        }
    }

}
