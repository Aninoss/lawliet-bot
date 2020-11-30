package mysql.modules.suggestions;

import core.CustomObservableMap;
import modules.suggestions.SuggestionMessage;
import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.channel.ServerTextChannel;

import java.util.HashMap;
import java.util.Optional;

public class SuggestionsBean extends BeanWithServer {

    private final CustomObservableMap<Long, SuggestionMessage> messages;
    private boolean active;
    private Long channelId;

    public SuggestionsBean(ServerBean serverBean, boolean active, Long channelId, @NonNull HashMap<Long, SuggestionMessage> messages) {
        super(serverBean);
        this.messages = new CustomObservableMap<>(messages);
        this.active = active;
        this.channelId = channelId;
    }


    /* Getters */

    public boolean isActive() {
        return active;
    }

    public CustomObservableMap<Long, SuggestionMessage> getSuggestionMessages() {
        return messages;
    }

    public Optional<Long> getChannelId() {
        return Optional.ofNullable(channelId);
    }

    public Optional<ServerTextChannel> getChannel() {
        return getServer().flatMap(server -> server.getTextChannelById(channelId != null ? channelId : 0L));
    }


    /* Setters */

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
