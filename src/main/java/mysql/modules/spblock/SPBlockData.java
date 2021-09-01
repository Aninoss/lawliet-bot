package mysql.modules.spblock;

import java.util.List;
import core.CustomObservableList;
import mysql.DataWithGuild;

public class SPBlockData extends DataWithGuild {

    public enum ActionList { DELETE_MESSAGE, KICK_USER, BAN_USER }

    private boolean active;
    private ActionList action;
    private final CustomObservableList<Long> ignoredUserIds;
    private final CustomObservableList<Long> ignoredChannelIds;
    private final CustomObservableList<Long> logReceiverUserIds;

    public SPBlockData(long serverId, boolean active, ActionList action, List<Long> ignoredUserIds,
                       List<Long> ignoredChannelIds, List<Long> logReceiverUserIds
    ) {
        super(serverId);
        this.active = active;
        this.action = action;
        this.ignoredUserIds = new CustomObservableList<>(ignoredUserIds);
        this.logReceiverUserIds = new CustomObservableList<>(logReceiverUserIds);
        this.ignoredChannelIds = new CustomObservableList<>(ignoredChannelIds);
    }

    public CustomObservableList<Long> getIgnoredUserIds() {
        return ignoredUserIds;
    }

    public CustomObservableList<Long> getLogReceiverUserIds() {
        return logReceiverUserIds;
    }

    public CustomObservableList<Long> getIgnoredChannelIds() {
        return ignoredChannelIds;
    }

    public boolean isActive() {
        return active;
    }

    public ActionList getAction() {
        return action;
    }

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

    public void setAction(ActionList action) {
        if (this.action != action) {
            this.action = action;
            setChanged();
            notifyObservers();
        }
    }

}
