package mysql.modules.ticket;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import core.CustomObservableList;
import core.CustomObservableMap;
import core.cache.ServerPatreonBoostCache;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class TicketData extends DataWithGuild {

    public enum TicketAssignmentMode {
        FIRST, EVERYONE, MANUAL
    }

    private Long channelId;
    private int counter;
    private boolean memberCanClose;
    private String createMessage;
    private TicketAssignmentMode ticketAssignmentMode;
    private boolean protocol;
    private boolean pingStaff;
    private boolean userMessages;
    private Integer autoCloseHours;
    private boolean deleteChannelOnTicketClose;
    private final CustomObservableList<Long> staffRoleIds;
    private final CustomObservableMap<Long, TicketChannel> ticketChannels;

    public TicketData(long serverId, Long channelId, int counter, boolean memberCanClose, String createMessage,
                      TicketAssignmentMode ticketAssignmentMode, boolean protocol, boolean pingStaff, boolean userMessages,
                      Integer autoCloseHours, boolean deleteChannelOnTicketClose, List<Long> staffRoleIds,
                      Map<Long, TicketChannel> ticketChannels
    ) {
        super(serverId);
        this.channelId = channelId;
        this.counter = counter;
        this.memberCanClose = memberCanClose;
        this.createMessage = createMessage;
        this.ticketAssignmentMode = ticketAssignmentMode;
        this.protocol = protocol;
        this.pingStaff = pingStaff;
        this.userMessages = userMessages;
        this.autoCloseHours = autoCloseHours;
        this.deleteChannelOnTicketClose = deleteChannelOnTicketClose;
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

    public void setMemberCanClose(boolean memberCanClose) {
        this.memberCanClose = memberCanClose;
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

    public TicketAssignmentMode getTicketAssignmentMode() {
        return ticketAssignmentMode;
    }

    public void setTicketAssignmentMode(TicketAssignmentMode ticketAssignmentMode) {
        this.ticketAssignmentMode = ticketAssignmentMode;
        setChanged();
        notifyObservers();
    }

    public boolean getProtocol() {
        return protocol;
    }

    public boolean getProtocolEffectively() {
        return getProtocol() && ServerPatreonBoostCache.get(getGuildId());
    }

    public void setProtocol(boolean protocol) {
        this.protocol = protocol;
        setChanged();
        notifyObservers();
    }

    public boolean getPingStaff() {
        return pingStaff;
    }

    public void setPingStaff(boolean pingStaff) {
        this.pingStaff = pingStaff;
        setChanged();
        notifyObservers();
    }

    public boolean getUserMessages() {
        return userMessages;
    }

    public void setUserMessages(boolean userMessages) {
        this.userMessages = userMessages;
        setChanged();
        notifyObservers();
    }

    public Integer getAutoCloseHours() {
        return autoCloseHours;
    }

    public Integer getAutoCloseHoursEffectively() {
        if (ServerPatreonBoostCache.get(getGuildId())) {
            return getAutoCloseHours();
        } else {
            return null;
        }
    }

    public void setAutoCloseHours(Integer autoCloseHours) {
        this.autoCloseHours = autoCloseHours;
        setChanged();
        notifyObservers();
    }

    public boolean getDeleteChannelOnTicketClose() {
        return deleteChannelOnTicketClose;
    }

    public void setDeleteChannelOnTicketClose(boolean deleteChannelOnTicketClose) {
        this.deleteChannelOnTicketClose = deleteChannelOnTicketClose;
        setChanged();
        notifyObservers();
    }

}
