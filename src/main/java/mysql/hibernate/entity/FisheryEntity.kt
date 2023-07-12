package mysql.hibernate.entity;

import core.assets.GuildAsset;
import mysql.hibernate.template.HibernateEmbeddedEntity;

import javax.persistence.Embeddable;

@Embeddable
public class FisheryEntity extends HibernateEmbeddedEntity<GuildEntity> implements GuildAsset {

    private long rolePriceMin = 50_000L;
    private long rolePriceMax = 800_000_000L;

    public long getRolePriceMin() {
        return rolePriceMin;
    }

    public void setRolePriceMin(long rolePriceMin) {
        this.rolePriceMin = rolePriceMin;
    }

    public long getRolePriceMax() {
        return rolePriceMax;
    }

    public void setRolePriceMax(long rolePriceMax) {
        this.rolePriceMax = rolePriceMax;
    }

    @Override
    public long getGuildId() {
        return getHibernateEntity().getGuildId();
    }
}
