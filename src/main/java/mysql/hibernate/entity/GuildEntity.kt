package mysql.hibernate.entity;

import constants.Language;
import core.assets.GuildAsset;
import core.cache.ServerPatreonBoostCache;
import mysql.hibernate.template.HibernateEntity;
import mysql.modules.guild.DBGuild;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Locale;

@Entity(name = "Guild")
public class GuildEntity extends HibernateEntity implements GuildAsset {

    @Id
    private String guildId;

    private String prefix = "L.";
    private String language = Language.EN.name();
    private boolean removeAuthorMessage = false;

    @Embedded
    FisheryEntity fishery = new FisheryEntity();

    public GuildEntity(String guildId) {
        this.guildId = guildId;
    }

    public GuildEntity(String guildId, String prefix, String language, boolean removeAuthorMessage) {
        this.guildId = guildId;
        this.prefix = prefix;
        this.language = language;
        this.removeAuthorMessage = removeAuthorMessage;
    }

    public GuildEntity() {
    }

    @Override
    public long getGuildId() {
        return Long.parseLong(guildId);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        DBGuild.getInstance().retrieve(Long.parseLong(guildId)).setPrefix(prefix); // TODO: remove after migration
    }

    public Language getLanguage() {
        return Language.valueOf(language);
    }

    public void setLanguage(Language language) {
        this.language = language.name();
        DBGuild.getInstance().retrieve(Long.parseLong(guildId)).setLocale(language.getLocale()); // TODO: remove after migration
    }

    public Locale getLocale() {
        return getLanguage().getLocale();
    }

    public boolean getRemoveAuthorMessage() {
        return removeAuthorMessage;
    }

    public boolean getRemoveAuthorMessageEffectively() {
        return getRemoveAuthorMessage() && ServerPatreonBoostCache.get(Long.parseLong(guildId));
    }

    public void setRemoveAuthorMessage(boolean removeAuthorMessage) {
        this.removeAuthorMessage = removeAuthorMessage;
        DBGuild.getInstance().retrieve(Long.parseLong(guildId)).setCommandAuthorMessageRemove(removeAuthorMessage); // TODO: remove after migration
    }

    public FisheryEntity getFishery() {
        fishery.setHibernateEntity(this);
        return fishery;
    }
}
