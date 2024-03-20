package mysql.modules.suggestions;

import core.CustomObservableMap;
import modules.suggestions.SuggestionMessage;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.Map;
import java.util.Optional;

public class SuggestionsData extends DataWithGuild {

    private final CustomObservableMap<Long, SuggestionMessage> messages;
    private boolean active;
    private Long channelId;

    public SuggestionsData(long serverId, boolean active, Long channelId, Map<Long, SuggestionMessage> messages) {
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

    public Optional<Long> getChannelId() {
        return Optional.ofNullable(channelId);
    }

    public Optional<GuildMessageChannel> getChannel() {
        return getGuild().map(guild -> guild.getChannelById(GuildMessageChannel.class, channelId != null ? channelId : 0L));
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
