package MySQL.BannedWords;

import General.DiscordApiCollection;
import General.CustomObservableList;
import MySQL.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class BannedWordsBean extends Observable {

    private long serverId;
    private boolean active;
    private ServerBean serverBean;
    private CustomObservableList<Long> ignoredUserIds, logReceiverUserIds;
    private CustomObservableList<String> words;

    public BannedWordsBean(long serverId, ServerBean serverBean, boolean active, @NonNull ArrayList<Long> ignoredUserIds, @NonNull ArrayList<Long> logReceiverUserIds, @NonNull ArrayList<String> words) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.active = active;
        this.ignoredUserIds = new CustomObservableList<>(ignoredUserIds);
        this.logReceiverUserIds = new CustomObservableList<>(logReceiverUserIds);
        this.words = new CustomObservableList<>(words);
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getBean() {
        return serverBean;
    }

    public CustomObservableList<Long> getIgnoredUserIds() {
        return ignoredUserIds;
    }

    public CustomObservableList<Long> getLogReceiverUserIds() {
        return logReceiverUserIds;
    }

    public CustomObservableList<String> getWords() {
        return words;
    }

    public boolean isActive() {
        return active;
    }


    /* Setters */

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

}
