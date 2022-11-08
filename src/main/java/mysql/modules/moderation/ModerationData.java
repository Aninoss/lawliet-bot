package mysql.modules.moderation;

import java.util.List;
import java.util.Optional;
import core.CustomObservableList;
import core.cache.ServerPatreonBoostCache;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class ModerationData extends DataWithGuild {

    private Long announcementChannelId;
    private boolean question;
    private Long muteRoleId;
    private boolean enforceMuteRole;
    private int autoKick;
    private int autoBan;
    private int autoMute;
    private int autoJail;
    private int autoKickDays;
    private int autoBanDays;
    private int autoMuteDays;
    private int autoJailDays;
    private int autoBanDuration;
    private int autoMuteDuration;
    private int autoJailDuration;
    private final CustomObservableList<Long> jailRoleIds;

    public ModerationData(long serverId, Long announcementChannelId, boolean question, Long muteRoleId,
                          boolean enforceMuteRole, int autoKick, int autoBan, int autoMute, int autoJail,
                          int autoKickDays, int autoBanDays, int autoMuteDays, int autoJailDays, int autoBanDuration,
                          int autoMuteDuration, int autoJailDuration, List<Long> jailRoleIds
    ) {
        super(serverId);
        this.announcementChannelId = announcementChannelId;
        this.question = question;
        this.muteRoleId = muteRoleId;
        this.enforceMuteRole = enforceMuteRole;
        this.autoKick = autoKick;
        this.autoBan = autoBan;
        this.autoMute = autoMute;
        this.autoJail = autoJail;
        this.autoKickDays = autoKickDays;
        this.autoBanDays = autoBanDays;
        this.autoMuteDays = autoMuteDays;
        this.autoJailDays = autoJailDays;
        this.autoBanDuration = autoBanDuration;
        this.autoMuteDuration = autoMuteDuration;
        this.autoJailDuration = autoJailDuration;
        this.jailRoleIds = new CustomObservableList<>(jailRoleIds);
        if (this.announcementChannelId != null && this.announcementChannelId == 0L) {
            this.announcementChannelId = null;
        }
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

    public boolean getQuestion() {
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

    public int getAutoJail() {
        return autoJail;
    }

    public int getAutoJailDays() {
        return autoJailDays;
    }

    public int getAutoJailDuration() {
        return autoJailDuration;
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

    public void setQuestion(boolean question) {
        if (question != this.question) {
            toggleQuestion();
        }
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

    public void setAutoJail(int autoJail, int autoJailDays, int autoJailDuration) {
        if (this.autoJail != autoJail || this.autoJailDays != autoJailDays || this.autoJailDuration != autoJailDuration) {
            this.autoJail = autoJail;
            this.autoJailDays = autoJailDays;
            this.autoJailDuration = autoJailDuration;
            setChanged();
            notifyObservers();
        }
    }

    public CustomObservableList<Long> getJailRoleIds() {
        return jailRoleIds;
    }

    public boolean getEnforceMuteRole() {
        return enforceMuteRole;
    }

    public boolean getEnforceMuteRoleEffectively() {
        return getEnforceMuteRole() && ServerPatreonBoostCache.get(getGuildId());
    }

    public void toggleEnforceMuteRole() {
        this.enforceMuteRole = !this.enforceMuteRole;
        setChanged();
        notifyObservers();
    }

}
