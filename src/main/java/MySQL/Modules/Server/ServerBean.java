package MySQL.Modules.Server;

import Constants.FisheryStatus;
import Core.DiscordApiCollection;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import java.util.Locale;
import java.util.Observable;
import java.util.Optional;

public class ServerBean extends Observable {

    private final long serverId;
    private long fisheryRoleMin, fisheryRoleMax;
    private String prefix, webhookUrl;
    private Locale locale;
    private FisheryStatus fisheryStatus;
    private boolean fisherySingleRoles, fisheryTreasureChests, fisheryReminders, commandAuthorMessageRemove, fisheryCoinsGivenLimit;
    private Long fisheryAnnouncementChannelId;
    private Integer fisheryVcHoursCap = null;

    public ServerBean(long serverId, String prefix, Locale locale, FisheryStatus fisheryStatus, boolean fisherySingleRoles,
                      Long fisheryAnnouncementChannelId, boolean fisheryTreasureChests, boolean fisheryReminders, long fisheryRoleMin, long fisheryRoleMax,
                      int fisheryVcHoursCap, String webhookUrl, boolean commandAuthorMessageRemove, boolean fisheryCoinsGivenLimit) {
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
        if (fisheryVcHoursCap == 0) this.fisheryVcHoursCap = null;
        else this.fisheryVcHoursCap = fisheryVcHoursCap;
        this.fisheryAnnouncementChannelId = fisheryAnnouncementChannelId != null && fisheryAnnouncementChannelId != 0 ? fisheryAnnouncementChannelId : null;
        this.commandAuthorMessageRemove = commandAuthorMessageRemove;
        this.fisheryCoinsGivenLimit = fisheryCoinsGivenLimit;
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() {
        return DiscordApiCollection.getInstance().getServerById(serverId);
    }

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

    public Optional<Integer> getFisheryVcHoursCap() {
        return Optional.ofNullable(fisheryVcHoursCap);
    }

    public boolean isCached() {
        return DBServer.getInstance().containsServerId(serverId);
    }

    public boolean isCommandAuthorMessageRemove() {
        return commandAuthorMessageRemove;
    }

    public boolean hasFisheryCoinsGivenLimit() {
        return fisheryCoinsGivenLimit;
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

    public void setFisheryVcHoursCap(Integer fisheryVcHoursCap) {
        if (this.fisheryVcHoursCap == null || !this.fisheryVcHoursCap.equals(fisheryVcHoursCap)) {
            this.fisheryVcHoursCap = fisheryVcHoursCap;
            setChanged();
            notifyObservers();
        }
    }

    public void setFisheryAnnouncementChannelId(Long fisheryAnnouncementChannelId) {
        if (this.fisheryAnnouncementChannelId == null || !this.fisheryAnnouncementChannelId.equals(fisheryAnnouncementChannelId)) {
            this.fisheryAnnouncementChannelId = fisheryAnnouncementChannelId;
            setChanged();
            notifyObservers();
        }
    }

    public void toggleCommandAuthorMessageRemove() {
        this.commandAuthorMessageRemove = !this.commandAuthorMessageRemove;
        setChanged();
        notifyObservers();
    }

    public void setCommandAuthorMessageRemove(boolean active) {
        if (this.isCommandAuthorMessageRemove() != active) toggleCommandAuthorMessageRemove();
    }

    public void toggleFisheryCoinsGivenLimit() {
        this.fisheryCoinsGivenLimit = !this.fisheryCoinsGivenLimit;
        setChanged();
        notifyObservers();
    }

}
