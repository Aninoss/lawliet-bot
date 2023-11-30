package mysql.hibernate.entity.guild

import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Embeddable

@Embeddable
class CustomCommandEntity(
        guildEntity: GuildEntity? = null,
        var textResponse: String = ""
) : HibernateEmbeddedEntity<GuildEntity>(guildEntity), HibernateDiscordInterface {

    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}