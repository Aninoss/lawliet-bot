package mysql.modules.spblock;

import core.CustomObservableList;
import mysql.BeanWithServer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

public class SPBlockBean extends BeanWithServer {

    public enum ActionList { DELETE_MESSAGE, KICK_USER, BAN_USER }

    private boolean active;
    private ActionList action;
    private final CustomObservableList<Long> ignoredUserIds, ignoredChannelIds, logReceiverUserIds;

    public SPBlockBean(long serverId, boolean active, ActionList action, @NonNull ArrayList<Long> ignoredUserIds, @NonNull ArrayList<Long> ignoredChannelIds, @NonNull ArrayList<Long> logReceiverUserIds) {
        super(serverId);
        this.active = active;
        this.action = action;
        this.ignoredUserIds = new CustomObservableList<>(ignoredUserIds);
        this.logReceiverUserIds = new CustomObservableList<>(logReceiverUserIds);
        this.ignoredChannelIds = new CustomObservableList<>(ignoredChannelIds);
    }


    /* Getters */

    public CustomObservableList<Long> getIgnoredUserIds() {
        return ignoredUserIds;
    }

    public CustomObservableList<Long> getLogReceiverUserIds() {
        return logReceiverUserIds;
    }

    public CustomObservableList<Long> getIgnoredChannelIds() { return ignoredChannelIds; }

    public boolean isActive() {
        return active;
    }

    public ActionList getAction() { return action; }


    /* Setters */

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
