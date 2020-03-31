package MySQL.Moderation;

import General.CustomObservableList;
import General.DiscordApiCollection;
import MySQL.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class ModerationBean extends Observable {

    private long serverId;
    private ServerBean serverBean;

    private Long announcementChannelId;
    private boolean question;
    private int autoKick, autoBan, autoKickDays, autoBanDays;

    public ModerationBean(long serverId, ServerBean serverBean, Long announcementChannelId, boolean question, int autoKick, int autoBan, int autoKickDays, int autoBanDays) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.announcementChannelId = announcementChannelId;
        this.question = question;
        this.autoKick = autoKick;
        this.autoBan = autoBan;
        this.autoKickDays = autoKickDays;
        this.autoBanDays = autoBanDays;
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getServerBean() {
        return serverBean;
    }

    public Optional<Long> getAnnouncementChannelId() {
        return Optional.ofNullable(announcementChannelId);
    }

    public Optional<ServerTextChannel> getAnnouncementChannel() {
        return getServer().flatMap(server -> server.getTextChannelById(announcementChannelId != null ? announcementChannelId : 0L));
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

    public void setServerBean(ServerBean serverBean) {
        this.serverBean = serverBean;
        setChanged();
        notifyObservers();
    }

    public void setAnnouncementChannelId(Long announcementChannelId) {
        this.announcementChannelId = announcementChannelId;
        setChanged();
        notifyObservers();
    }

    public void toggleQuestion() {
        this.question = !this.question;
        setChanged();
        notifyObservers();
    }

    public void setAutoKick(int autoKick, int autoKickDays) {
        this.autoKick = autoKick;
        this.autoKickDays = autoKickDays;
        setChanged();
        notifyObservers();
    }

    public void setAutoBan(int autoBan, int autoBanDays) {
        this.autoBan = autoBan;
        this.autoBanDays = autoBanDays;
        setChanged();
        notifyObservers();
    }

}
