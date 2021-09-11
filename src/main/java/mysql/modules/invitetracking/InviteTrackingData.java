package mysql.modules.invitetracking;

import java.util.Map;
import core.CustomObservableMap;
import mysql.DataWithGuild;

public class InviteTrackingData extends DataWithGuild {

    private final CustomObservableMap<Long, InviteTrackingSlot> inviteTrackingSlots;
    private final CustomObservableMap<String, GuildInvite> guildInvites;

    public InviteTrackingData(long serverId, Map<Long, InviteTrackingSlot> inviteTrackerSlots,
                              Map<String, GuildInvite> guildInvites
    ) {
        super(serverId);
        this.inviteTrackingSlots = new CustomObservableMap<>(inviteTrackerSlots);
        this.guildInvites = new CustomObservableMap<>(guildInvites);
    }

    public CustomObservableMap<Long, InviteTrackingSlot> getInviteTrackingSlots() {
        return inviteTrackingSlots;
    }

    public CustomObservableMap<String, GuildInvite> getGuildInvites() {
        return guildInvites;
    }

}
