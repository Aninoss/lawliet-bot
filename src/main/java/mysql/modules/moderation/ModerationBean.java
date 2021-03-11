package mysql.modules.moderation;

import java.util.Optional;
import mysql.BeanWithGuild;
import net.dv8tion.jda.api.entities.TextChannel;

public class ModerationBean extends BeanWithGuild {

    private Long announcementChannelId;
    private boolean question;
    private int autoKick, autoBan, autoKickDays, autoBanDays;

    public ModerationBean(long serverId, Long announcementChannelId, boolean question, int autoKick, int autoBan, int autoKickDays, int autoBanDays) {
        super(serverId);
        this.announcementChannelId = announcementChannelId;
        this.question = question;
        this.autoKick = autoKick;
        this.autoBan = autoBan;
        this.autoKickDays = autoKickDays;
        this.autoBanDays = autoBanDays;
    }


    /* Getters */

    public Optional<Long> getAnnouncementChannelId() {
        return Optional.ofNullable(announcementChannelId);
    }

    public Optional<TextChannel> getAnnouncementChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(announcementChannelId != null ? announcementChannelId : 0L));
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


    /* Setters */

    public void setAnnouncementChannelId(Long announcementChannelId) {
        if (this.announcementChannelId == null || !this.announcementChannelId.equals(announcementChannelId)) {
            this.announcementChannelId = announcementChannelId;
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
