package mysql.hibernate.entity

import core.assets.GuildAsset
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Embeddable

@Embeddable
class FisheryEntity : HibernateEmbeddedEntity<GuildEntity>(), GuildAsset {

    var rolePriceMin = 50_000L
    var rolePriceMax = 800_000_000L

    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
