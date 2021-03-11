package mysql.modules.guild;

import java.util.Locale;
import java.util.Optional;
import constants.FisheryStatus;
import mysql.BeanWithGuild;
import net.dv8tion.jda.api.entities.TextChannel;

public class GuildBean extends BeanWithGuild {

    private final long guildId;
    private long fisheryRoleMin, fisheryRoleMax;
    private String prefix;
    private Locale locale;
    private FisheryStatus fisheryStatus;
    private boolean fisherySingleRoles;
    private boolean fisheryTreasureChests;
    private boolean fisheryReminders;
    private boolean commandAuthorMessageRemove;
    private boolean fisheryCoinsGivenLimit;
    private Long fisheryAnnouncementChannelId;
    private Integer fisheryVcHoursCap;

    public GuildBean(long guildId, String prefix, Locale locale, FisheryStatus fisheryStatus, boolean fisherySingleRoles,
                     Long fisheryAnnouncementChannelId, boolean fisheryTreasureChests, boolean fisheryReminders, long fisheryRoleMin, long fisheryRoleMax,
                     int fisheryVcHoursCap, boolean commandAuthorMessageRemove, boolean fisheryCoinsGivenLimit) {
        super(guildId);
        this.guildId = guildId;
        this.fisheryRoleMin = fisheryRoleMin;
        this.fisheryRoleMax = fisheryRoleMax;
        this.prefix = prefix;
        this.locale = locale;
        this.fisheryStatus = fisheryStatus;
        this.fisherySingleRoles = fisherySingleRoles;
        this.fisheryTreasureChests = fisheryTreasureChests;
        this.fisheryReminders = fisheryReminders;
        if (fisheryVcHoursCap == 0) {
            this.fisheryVcHoursCap = null;
        } else {
            this.fisheryVcHoursCap = fisheryVcHoursCap;
        }
        this.fisheryAnnouncementChannelId = fisheryAnnouncementChannelId != null && fisheryAnnouncementChannelId != 0 ? fisheryAnnouncementChannelId : null;
        this.commandAuthorMessageRemove = commandAuthorMessageRemove;
        this.fisheryCoinsGivenLimit = fisheryCoinsGivenLimit;
    }


    /* Getters */

    public long getFisheryRoleMin() {
        return fisheryRoleMin;
    }

    public long getFisheryRoleMax() {
        return fisheryRoleMax;
    }

    public String getPrefix() {
        return prefix;
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

    public Optional<TextChannel> getFisheryAnnouncementChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(fisheryAnnouncementChannelId != null ? fisheryAnnouncementChannelId : 0L));
    }

    public Optional<Integer> getFisheryVcHoursCap() {
        return Optional.ofNullable(fisheryVcHoursCap);
    }

    public boolean isSaved() {
        return DBGuild.getInstance().containsServerId(guildId);
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
