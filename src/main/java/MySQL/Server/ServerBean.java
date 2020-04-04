package MySQL.Server;

import Constants.FisheryStatus;
import General.DiscordApiCollection;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import java.util.Locale;
import java.util.Observable;
import java.util.Optional;

public class ServerBean extends Observable {

    private long serverId, fisheryRoleMin, fisheryRoleMax;
    private String prefix, webhookUrl;
    private Locale locale;
    private FisheryStatus fisheryStatus;
    private boolean fisherySingleRoles, fisheryTreasureChests, fisheryReminders;
    private Long fisheryAnnouncementChannelId;

    public ServerBean(long serverId, String prefix, Locale locale, FisheryStatus fisheryStatus, boolean fisherySingleRoles, Long fisheryAnnouncementChannelId, boolean fisheryTreasureChests, boolean fisheryReminders, long fisheryRoleMin, long fisheryRoleMax, String webhookUrl) {
        this.serverId = serverId;
        this.fisheryRoleMin = fisheryRoleMin;
        this.fisheryRoleMax = fisheryRoleMax;
        this.prefix = prefix;
        this.webhookUrl = webhookUrl;
        this.locale = locale;
        this.fisheryStatus = fisheryStatus;
        this.fisherySingleRoles = fisherySingleRoles;
        this.fisheryTreasureChests = fisheryTreasureChests;
        this.fisheryReminders = fisheryReminders;
        this.fisheryAnnouncementChannelId = fisheryAnnouncementChannelId != null && fisheryAnnouncementChannelId != 0 ? fisheryAnnouncementChannelId : null;
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public long getFisheryRoleMin() {
        return fisheryRoleMin;
    }

    public long getFisheryRoleMax() {
        return fisheryRoleMax;
    }

    public String getPrefix() {
        return prefix;
    }

    public Optional<String> getWebhookUrl() {
        return Optional.ofNullable(webhookUrl);
    }

    public Locale getLocale() {
        return locale;
    }

    public FisheryStatus getFisheryStatus() {
        return fisheryStatus;
    }

    public boolean isFisherySingleRoles() {
        return fisherySingleRoles;
    }

    public boolean isFisheryTreasureChests() {
        return fisheryTreasureChests;
    }

    public boolean isFisheryReminders() {
        return fisheryReminders;
    }

    public Optional<Long> getFisheryAnnouncementChannelId() {
        return Optional.ofNullable(fisheryAnnouncementChannelId);
    }

    public Optional<ServerTextChannel> getFisheryAnnouncementChannel() {
        return getServer().flatMap(server -> server.getTextChannelById(fisheryAnnouncementChannelId != null ? fisheryAnnouncementChannelId : 0L));
    }


    /* Setters */

    public void setFisheryRolePrices(long priceMin, long priceMax) {
        if (this.fisheryRoleMin != priceMin || this.fisheryRoleMax != priceMax) {
            this.fisheryRoleMin = priceMin;
            this.fisheryRoleMax = priceMax;
            setChanged();
            notifyObservers();
        }
    }

    public void setPrefix(String prefix) {
        if (this.prefix == null || !this.prefix.equals(prefix)) {
            this.prefix = prefix;
            setChanged();
            notifyObservers();
        }
    }

    public void setWebhookUrl(String webhookUrl) {
        if (this.webhookUrl == null || !this.webhookUrl.equals(webhookUrl)) {
            this.webhookUrl = webhookUrl;
            setChanged();
            notifyObservers();
        }
    }

    public void setLocale(Locale locale) {
        if (this.locale == null || !this.locale.equals(locale)) {
            this.locale = locale;
            setChanged();
            notifyObservers();
        }
    }

    public void setFisheryStatus(FisheryStatus fisheryStatus) {
        if (this.fisheryStatus == null || !this.fisheryStatus.equals(fisheryStatus)) {
            this.fisheryStatus = fisheryStatus;
            setChanged();
            notifyObservers();
        }
    }

    public void toggleFisherySingleRoles() {
        this.fisherySingleRoles = !this.fisherySingleRoles;
        setChanged();
        notifyObservers();
    }

    public void toggleFisheryTreasureChests() {
        this.fisheryTreasureChests = !this.fisheryTreasureChests;
        setChanged();
        notifyObservers();
    }

    public void toggleFisheryReminders() {
        this.fisheryReminders = !this.fisheryReminders;
        setChanged();
        notifyObservers();
    }

    public void setFisheryAnnouncementChannelId(Long fisheryAnnouncementChannelId) {
        if (this.fisheryAnnouncementChannelId == null || !this.fisheryAnnouncementChannelId.equals(fisheryAnnouncementChannelId)) {
            this.fisheryAnnouncementChannelId = fisheryAnnouncementChannelId;
            setChanged();
            notifyObservers();
        }
    }

}
