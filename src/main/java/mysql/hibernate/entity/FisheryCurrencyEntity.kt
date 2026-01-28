package mysql.hibernate.entity

import mysql.hibernate.entity.assets.NullableEmojiAsset
import mysql.hibernate.entity.guild.GuildEntity
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Embeddable

@Embeddable
class FisheryCurrencyEntity(override var emojiFormatted: String? = null, var name: String? = null) : HibernateEmbeddedEntity<GuildEntity>(), NullableEmojiAsset {
}