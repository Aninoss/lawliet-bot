package mysql.modules.autochannel;

import java.util.List;
import java.util.Optional;
import core.CustomObservableList;
import core.MainLogger;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class AutoChannelData extends DataWithGuild {

    private boolean active, locked;
    private String nameMask;
    private final CustomObservableList<Long> childChannels;
    private Long parentChannelId;

    public AutoChannelData(long serverId, Long parentChannelId, boolean active, String nameMask, boolean locked,
                           List<Long> childChannels
    ) {
        super(serverId);
        this.parentChannelId = parentChannelId;
        this.active = active;
        this.nameMask = nameMask;
        this.locked = locked;
        this.childChannels = new CustomObservableList<>(childChannels);
    }

    public Optional<Long> getParentChannelId() {
        return Optional.ofNullable(parentChannelId);
    }

    public Optional<VoiceChannel> getParentChannel() {
        return getGuild().map(guild -> guild.getVoiceChannelById(parentChannelId != null ? parentChannelId : 0L));
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
