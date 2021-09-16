package mysql.modules.invitetracking;

import java.util.Map;
import java.util.Optional;
import core.CustomObservableMap;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.TextChannel;

public class InviteTrackingData extends DataWithGuild {

    private boolean active;
    private Long channelId;
    private boolean ping;
    private boolean advanced;
    private final CustomObservableMap<Long, InviteTrackingSlot> inviteTrackingSlots;
    private final CustomObservableMap<String, GuildInvite> guildInvites;

    public InviteTrackingData(long serverId, boolean active, Long channelId, boolean ping, boolean advanced,
                              Map<Long, InviteTrackingSlot> inviteTrackerSlots, Map<String, GuildInvite> guildInvites
    ) {
        super(serverId);
        this.active = active;
        this.channelId = channelId;
        this.ping = ping;
        this.advanced = advanced;
        this.inviteTrackingSlots = new CustomObservableMap<>(inviteTrackerSlots);
        this.guildInvites = new CustomObservableMap<>(guildInvites);
    }

    public CustomObservableMap<Long, InviteTrackingSlot> getInviteTrackingSlots() {
        return inviteTrackingSlots;
    }

    public CustomObservableMap<String, GuildInvite> getGuildInvites() {
        return guildInvites;
    }

    public boolean isActive() {
        return active;
    }

    public void toggleActive() {
        this.active = !this.active;
        setChanged();
        notifyObservers();
    }

    public Optional<Long> getTextChannelId() {
        return Optional.ofNullable(channelId);
    }

    public Optional<TextChannel> getTextChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(channelId != null ? channelId : 0L));
    }

    public void setChannelId(Long channelId) {
        if (this.channelId == null || !this.channelId.equals(channelId)) {
            this.channelId = channelId;
            setChanged();
            notifyObservers();
        }
    }

    public boolean getPing() {
        return ping;
    }

    public void togglePing() {
        this.ping = !this.ping;
        setChanged();
        notifyObservers();
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void toggleAdvanced() {
        this.advanced = !this.advanced;
        setChanged();
        notifyObservers();
    }

}
