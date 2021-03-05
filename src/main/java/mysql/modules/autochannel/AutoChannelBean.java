package mysql.modules.autochannel;

import core.CustomObservableList;
import mysql.BeanWithServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;

public class AutoChannelBean extends BeanWithServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(AutoChannelBean.class);

    private boolean active, locked;
    private String nameMask;
    private final CustomObservableList<Long> childChannels;
    private Long parentChannelId;

    public AutoChannelBean(long serverId, Long parentChannelId, boolean active, String nameMask, boolean locked, @NonNull ArrayList<Long> childChannels) {
        super(serverId);
        this.parentChannelId = parentChannelId;
        this.active = active;
        this.nameMask = nameMask;
        this.locked = locked;
        this.childChannels = new CustomObservableList<>(childChannels);
    }


    /* Getters */

    public Optional<Long> getParentChannelId() {
        return Optional.ofNullable(parentChannelId);
    }

    public Optional<ServerVoiceChannel> getParentChannel() {
        return getServer().flatMap(server -> server.getVoiceChannelById(parentChannelId != null ? parentChannelId : 0L));
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getNameMask() {
        return nameMask;
    }

    public synchronized CustomObservableList<Long> getChildChannelIds() {
        if (childChannels == null) MainLogger.get().error("Child channels is null");
        return childChannels;
    }


    /* Setters */

    public void setParentChannelId(Long parentChannelId) {
        if (this.parentChannelId == null || !this.parentChannelId.equals(parentChannelId)) {
            this.parentChannelId = parentChannelId;
            setChanged();
            notifyObservers();
        }
    }

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

    public void toggleLocked() {
        this.locked = !this.locked;
        setChanged();
        notifyObservers();
    }

    public void setNameMask(String nameMask) {
        if (this.nameMask == null || !this.nameMask.equals(nameMask)) {
            this.nameMask = nameMask;
            setChanged();
            notifyObservers();
        }
    }

}
