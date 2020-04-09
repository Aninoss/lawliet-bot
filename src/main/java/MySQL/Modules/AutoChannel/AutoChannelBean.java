package MySQL.Modules.AutoChannel;

import Core.DiscordApiCollection;
import Core.CustomObservableList;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class AutoChannelBean extends Observable {

    private long serverId;
    private boolean active, locked;
    private String nameMask;
    private CustomObservableList<Long> childChannels;
    private Long parentChannelId;
    private ServerBean serverBean;

    public AutoChannelBean(long serverId, ServerBean serverBean, Long parentChannelId, boolean active, String nameMask, boolean locked, @NonNull ArrayList<Long> childChannels) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.parentChannelId = parentChannelId;
        this.active = active;
        this.nameMask = nameMask;
        this.locked = locked;
        this.childChannels = new CustomObservableList<>(childChannels);
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getServerBean() {
        return serverBean;
    }

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

    public CustomObservableList<Long> getChildChannels() {
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
