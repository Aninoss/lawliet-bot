package mysql.modules.ticket;

import java.util.ArrayList;
import java.util.Optional;
import core.CustomObservableList;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TicketData extends DataWithGuild {

    private Long channelId;
    private final CustomObservableList<Long> staffRoleIds;
    private final CustomObservableList<Long> openTextChannelIds;

    public TicketData(long serverId, Long channelId, @NonNull ArrayList<Long> staffRoleIds, @NonNull ArrayList<Long> openTextChannelIds) {
        super(serverId);
        this.channelId = channelId;
        this.staffRoleIds = new CustomObservableList<>(staffRoleIds);
        this.openTextChannelIds = new CustomObservableList<>(openTextChannelIds);
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

    public CustomObservableList<Long> getStaffRoleIds() {
        return staffRoleIds;
    }

    public CustomObservableList<Long> getOpenTextChannelIds() {
        return openTextChannelIds;
    }

}
