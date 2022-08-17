package mysql.modules.bannedwords;

import java.util.List;
import core.CustomObservableList;
import mysql.DataWithGuild;

public class BannedWordsData extends DataWithGuild {

    private boolean active;
    private final CustomObservableList<Long> ignoredUserIds, logReceiverUserIds;
    private final CustomObservableList<String> words;

    public BannedWordsData(long serverId, boolean active, List<Long> ignoredUserIds, List<Long> logReceiverUserIds,
                           List<String> words
    ) {
        super(serverId);
        this.active = active;
        this.ignoredUserIds = new CustomObservableList<>(ignoredUserIds);
        this.logReceiverUserIds = new CustomObservableList<>(logReceiverUserIds);
        this.words = new CustomObservableList<>(words);
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

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            toggleActive();
        }
    }

}
