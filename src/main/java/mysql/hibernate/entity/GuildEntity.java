package mysql.hibernate.entity;

import constants.Language;
import core.assets.GuildAsset;
import core.cache.ServerPatreonBoostCache;
import mysql.modules.guild.DBGuild;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Locale;

@Entity(name = "Guild")
public class GuildEntity extends HibernateEntity implements GuildAsset {

    @Id
    private long guildId;

    private String prefix = "L.";
    private String language = Language.EN.name();
    private boolean big = false;
    private boolean removeAuthorMessage = false;

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
        DBGuild.getInstance().retrieve(guildId).setPrefix(prefix); // TODO: remove after migration
    }

    public Language getLanguage() {
        return Language.valueOf(language);
    }

    public void setLanguage(Language language) {
        this.language = language.name();
        DBGuild.getInstance().retrieve(guildId).setLocale(language.getLocale()); // TODO: remove after migration
    }

    public Locale getLocale() {
        return getLanguage().getLocale();
    }

    public boolean getBig() {
        return big;
    }

    public void setBig(boolean big) {
        this.big = big;
        DBGuild.getInstance().retrieve(guildId).setBig(big); // TODO: remove after migration
    }

    public boolean getRemoveAuthorMessage() {
        return removeAuthorMessage;
    }

    public boolean getRemoveAuthorMessageEffectively() {
        return getRemoveAuthorMessage() && ServerPatreonBoostCache.get(getGuildId());
    }

    public void setRemoveAuthorMessage(boolean removeAuthorMessage) {
        this.removeAuthorMessage = removeAuthorMessage;
        DBGuild.getInstance().retrieve(guildId).setCommandAuthorMessageRemove(removeAuthorMessage); // TODO: remove after migration
    }

}
