package mysql.modules.moderation;

import java.util.Optional;
import mysql.BeanWithGuild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class ModerationBean extends BeanWithGuild {

    private Long announcementChannelId;
    private boolean question;
    private Long muteRoleId;
    private int autoKick;
    private int autoBan;
    private int autoKickDays;
    private int autoBanDays;

    public ModerationBean(long serverId, Long announcementChannelId, boolean question, Long muteRoleId, int autoKick, int autoBan, int autoKickDays, int autoBanDays) {
        super(serverId);
        this.announcementChannelId = announcementChannelId;
        this.question = question;
        this.muteRoleId = muteRoleId;
        this.autoKick = autoKick;
        this.autoBan = autoBan;
        this.autoKickDays = autoKickDays;
        this.autoBanDays = autoBanDays;
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

    public void setAutoBan(int autoBan, int autoBanDays) {
        if (this.autoBan != autoBan || this.autoBanDays != autoBanDays) {
            this.autoBan = autoBan;
            this.autoBanDays = autoBanDays;
            setChanged();
            notifyObservers();
        }
    }

}
