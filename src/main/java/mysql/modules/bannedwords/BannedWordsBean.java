package mysql.modules.bannedwords;

import core.CustomObservableList;
import mysql.BeanWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

public class BannedWordsBean extends BeanWithGuild {

    private boolean active;
    private final CustomObservableList<Long> ignoredUserIds, logReceiverUserIds;
    private final CustomObservableList<String> words;

    public BannedWordsBean(long serverId, boolean active, @NonNull ArrayList<Long> ignoredUserIds, @NonNull ArrayList<Long> logReceiverUserIds, @NonNull ArrayList<String> words) {
        super(serverId);
        this.active = active;
        this.ignoredUserIds = new CustomObservableList<>(ignoredUserIds);
        this.logReceiverUserIds = new CustomObservableList<>(logReceiverUserIds);
        this.words = new CustomObservableList<>(words);
    }


    /* Getters */

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
