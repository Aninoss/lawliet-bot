package mysql.modules.ticket;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import core.CustomObservableList;
import core.CustomObservableMap;
import core.cache.ServerPatreonBoostCache;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.TextChannel;

public class TicketData extends DataWithGuild {

    private Long channelId;
    private int counter;
    private boolean memberCanClose;
    private String createMessage;
    private boolean assignToAll;
    private boolean protocol;
    private boolean pingStaff;
    private boolean userMessages;
    private final CustomObservableList<Long> staffRoleIds;
    private final CustomObservableMap<Long, TicketChannel> ticketChannels;

    public TicketData(long serverId, Long channelId, int counter, boolean memberCanClose, String createMessage,
                      boolean assignToAll, boolean protocol, boolean pingStaff, boolean userMessages,
                      List<Long> staffRoleIds, Map<Long, TicketChannel> ticketChannels
    ) {
        super(serverId);
        this.channelId = channelId;
        this.counter = counter;
        this.memberCanClose = memberCanClose;
        this.createMessage = createMessage;
        this.assignToAll = assignToAll;
        this.protocol = protocol;
        this.pingStaff = pingStaff;
        this.userMessages = userMessages;
        this.staffRoleIds = new CustomObservableList<>(staffRoleIds);
        this.ticketChannels = new CustomObservableMap<>(ticketChannels);
    }

    public Optional<Long> getAnnouncementTextChannelId() {
        return Optional.ofNullable(channelId);
    }

    public Optional<TextChannel> getAnnouncementTextChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(channelId != null ? channelId : 0L));
    }

    public void setAnnouncementTextChannelId(Long channelId) {
        if (this.channelId == null || !this.channelId.equals(channelId)) {
            this.channelId = channelId;
            setChanged();
            notifyObservers();
        }
    }

    public int increaseCounterAndGet() {
        counter++;
        if (counter > 9999) {
            counter = 1;
        }
        setChanged();
        notifyObservers();
        return counter;
    }

    public int getCounter() {
        return counter;
    }

    public CustomObservableList<Long> getStaffRoleIds() {
        return staffRoleIds;
    }

    public CustomObservableMap<Long, TicketChannel> getTicketChannels() {
        return ticketChannels;
    }

    public void toggleMemberCanClose() {
        this.memberCanClose = !this.memberCanClose;
        setChanged();
        notifyObservers();
    }

    public boolean memberCanClose() {
        return memberCanClose;
    }

    public Optional<String> getCreateMessage() {
        return Optional.ofNullable(createMessage);
    }

    public void setCreateMessage(String createMessage) {
        if (this.createMessage == null || !this.createMessage.equals(createMessage)) {
            this.createMessage = createMessage;
            setChanged();
            notifyObservers();
        }
    }

    public boolean getAssignToAll() {
        return assignToAll;
    }

    public void toggleAssignToAll() {
        this.assignToAll = !this.assignToAll;
        setChanged();
        notifyObservers();
    }

    public boolean getProtocol() {
        return protocol;
    }

    public boolean getProtocolEffectively() {
        return getProtocol() && ServerPatreonBoostCache.get(getGuildId());
    }

    public void toggleProtocol() {
        this.protocol = !this.protocol;
        setChanged();
        notifyObservers();
    }

    public boolean getPingStaff() {
        return pingStaff;
    }

    public void togglePingStaff() {
        this.pingStaff = !this.pingStaff;
        setChanged();
        notifyObservers();
    }

    public boolean getUserMessages() {
        return userMessages;
    }

    public void toggleUserMessages() {
        this.userMessages = !this.userMessages;
        setChanged();
        notifyObservers();
    }

}
