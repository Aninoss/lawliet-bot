package mysql.modules.ticket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import core.CustomObservableList;
import core.CustomObservableMap;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TicketData extends DataWithGuild {

    private Long channelId;
    private int counter;
    private final CustomObservableList<Long> staffRoleIds;
    private final CustomObservableMap<Long, TicketChannel> ticketChannels;

    public TicketData(long serverId, Long channelId, int counter, @NonNull ArrayList<Long> staffRoleIds, @NonNull HashMap<Long, TicketChannel> ticketChannels) {
        super(serverId);
        this.channelId = channelId;
        this.counter = counter;
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

}
