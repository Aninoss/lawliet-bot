package mysql.modules.suggestions;

import core.CustomObservableList;
import modules.suggestions.SuggestionMessage;
import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.channel.ServerVoiceChannel;

import java.util.ArrayList;
import java.util.Optional;

public class SuggestionsBean extends BeanWithServer {

    private final CustomObservableList<SuggestionMessage> messages;
    private boolean active;
    private Long channelId;

    public SuggestionsBean(ServerBean serverBean, boolean active, Long channelId, @NonNull ArrayList<SuggestionMessage> messages) {
        super(serverBean);
        this.messages = new CustomObservableList<>(messages);
        this.active = active;
        this.channelId = channelId;
    }


    /* Getters */

    public boolean isActive() {
        return active;
    }

    public CustomObservableList<SuggestionMessage> getSuggestionMessages() {
        return messages;
    }

    public Optional<Long> getChannelId() {
        return Optional.ofNullable(channelId);
    }

    public Optional<ServerVoiceChannel> getChannel() {
        return getServer().flatMap(server -> server.getVoiceChannelById(channelId != null ? channelId : 0L));
    }


    /* Setters */

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

}
