package mysql.modules.moderation;

import java.util.List;
import java.util.Optional;
import core.CustomObservableList;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ModerationData extends DataWithGuild {

    private Long announcementChannelId;
    private boolean question;
    private Long muteRoleId;
    private int autoKick;
    private int autoBan;
    private int autoMute;
    private int autoKickDays;
    private int autoBanDays;
    private int autoMuteDays;
    private int autoBanDuration;
    private int autoMuteDuration;
    private final CustomObservableList<Long> jailRoleIds;

    public ModerationData(long serverId, Long announcementChannelId, boolean question, Long muteRoleId, int autoKick,
                          int autoBan, int autoMute, int autoKickDays, int autoBanDays, int autoMuteDays,
                          int autoBanDuration, int autoMuteDuration, @NonNull List<Long> jailRoleIds
    ) {
        super(serverId);
        this.announcementChannelId = announcementChannelId;
        this.question = question;
        this.muteRoleId = muteRoleId;
        this.autoKick = autoKick;
        this.autoBan = autoBan;
        this.autoMute = autoMute;
        this.autoKickDays = autoKickDays;
        this.autoBanDays = autoBanDays;
        this.autoMuteDays = autoMuteDays;
        this.autoBanDuration = autoBanDuration;
        this.autoMuteDuration = autoMuteDuration;
        this.jailRoleIds = new CustomObservableList<>(jailRoleIds);
    }

    public Optional<Long> getAnnouncementChannelId() {
        return Optional.ofNullable(announcementChannelId);
    }

    public Optional<TextChannel> getAnnouncementChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(announcementChannelId != null ? announcementChannelId : 0L));
    }

    public Optional<Long> getMuteRoleId() {
        return Optional.ofNullable(muteRoleId);
    }

    public Optional<Role> getMuteRole() {
        return getGuild().map(guild -> guild.getRoleById(muteRoleId != null ? muteRoleId : 0L));
    }

    public boolean isQuestion() {
        return question;
    }

    public int getAutoKick() {
        return autoKick;
    }

    public int getAutoBan() {
        return autoBan;
    }

    public int getAutoKickDays() {
        return autoKickDays;
    }

    public int getAutoBanDays() {
        return autoBanDays;
    }

    public int getAutoMute() {
        return autoMute;
    }

    public int getAutoMuteDays() {
        return autoMuteDays;
    }

    public int getAutoBanDuration() {
        return autoBanDuration;
    }

    public int getAutoMuteDuration() {
        return autoMuteDuration;
    }

    public void setAnnouncementChannelId(Long announcementChannelId) {
        if (this.announcementChannelId == null || !this.announcementChannelId.equals(announcementChannelId)) {
            this.announcementChannelId = announcementChannelId;
            setChanged();
            notifyObservers();
        }
    }

    public void setMuteRoleId(Long muteRoleId) {
        if (this.muteRoleId == null || !this.muteRoleId.equals(muteRoleId)) {
            this.muteRoleId = muteRoleId;
            setChanged();
            notifyObservers();
        }
    }

    public void toggleQuestion() {
        this.question = !this.question;
        setChanged();
        notifyObservers();
    }

    public void setAutoKick(int autoKick, int autoKickDays) {
        if (this.autoKick != autoKick || this.autoKickDays != autoKickDays) {
            this.autoKick = autoKick;
            this.autoKickDays = autoKickDays;
            setChanged();
            notifyObservers();
        }
    }

    public void setAutoBan(int autoBan, int autoBanDays, int autoBanDuration) {
        if (this.autoBan != autoBan || this.autoBanDays != autoBanDays || this.autoBanDuration != autoBanDuration) {
            this.autoBan = autoBan;
            this.autoBanDays = autoBanDays;
            this.autoBanDuration = autoBanDuration;
            setChanged();
            notifyObservers();
        }
    }

    public void setAutoMute(int autoMute, int autoMuteDays, int autoMuteDuration) {
        if (this.autoMute != autoMute || this.autoMuteDays != autoMuteDays || this.autoMuteDuration != autoMuteDuration) {
            this.autoMute = autoMute;
            this.autoMuteDays = autoMuteDays;
            this.autoMuteDuration = autoMuteDuration;
            setChanged();
            notifyObservers();
        }
    }

    public CustomObservableList<Long> getJailRoleIds() {
        return jailRoleIds;
    }

}
