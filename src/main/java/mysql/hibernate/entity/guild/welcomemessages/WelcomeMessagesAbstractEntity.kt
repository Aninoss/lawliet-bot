package mysql.hibernate.entity.guild.welcomemessages

import mysql.hibernate.entity.assets.CdnImageAsset
import mysql.hibernate.entity.guild.GuildEntity
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Embeddable

@Embeddable
abstract class WelcomeMessagesAbstractEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface, CdnImageAsset {

    abstract var active: Boolean

    abstract var text: String

    abstract var embeds: Boolean


    override val guildId: Long
        get() = hibernateEntity.guildId

    override fun getFileDir(): String {
        return "welcome_custom_images"
    }

}
