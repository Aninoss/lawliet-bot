package mysql.hibernate.entity;

import java.time.LocalDate;
import java.util.Locale;
import javax.persistence.Entity;
import javax.persistence.Id;
import constants.Language;
import core.assets.GuildAsset;
import core.cache.ServerPatreonBoostCache;

@Entity(name = "Guild")
public class GuildEntity implements GuildAsset {

    @Id
    private long guildId;

    private String prefix = "L.";
    private String language = Language.EN.name();
    private boolean big = false;
    private boolean removeAuthorMessage = false;
    private LocalDate kickedDate;

    public GuildEntity(Long guildId) {
        this.guildId = guildId;
    }

    public GuildEntity() {
    }

    public long getGuildId() {
        return guildId;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Language getLanguage() {
        return Language.valueOf(language);
    }

    public void setLanguage(Language language) {
        this.language = language.name();
    }

    public Locale getLocale() {
        return getLanguage().getLocale();
    }

    public boolean getBig() {
        return big;
    }

    public void setBig(boolean big) {
        this.big = big;
    }

    public boolean getRemoveAuthorMessage() {
        return removeAuthorMessage;
    }

    public boolean getRemoveAuthorMessageEffectively() {
        return getRemoveAuthorMessage() && ServerPatreonBoostCache.get(getGuildId());
    }

    public void setRemoveAuthorMessage(boolean removeAuthorMessage) {
        this.removeAuthorMessage = removeAuthorMessage;
    }

    public LocalDate getKickedDate() {
        return kickedDate;
    }

    public void setKickedDate(LocalDate kickedDate) {
        this.kickedDate = kickedDate;
    }

}
