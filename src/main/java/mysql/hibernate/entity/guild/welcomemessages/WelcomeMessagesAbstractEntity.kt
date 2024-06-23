package mysql.hibernate.entity.guild.welcomemessages

import core.LocalFile
import mysql.hibernate.entity.assets.CdnImageAsset
import mysql.hibernate.entity.guild.GuildEntity
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import java.io.File
import javax.persistence.Embeddable

@Embeddable
abstract class WelcomeMessagesAbstractEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface, CdnImageAsset {

    abstract var active: Boolean

    abstract var text: String

    abstract var embeds: Boolean


    override val guildId: Long
        get() = hibernateEntity.guildId

    override fun getFileDir(): String {
        return "welcome_images"
    }

    fun getImageFile(): File? {
        if (imageFilename == null) {
            return null
        }
        return LocalFile(LocalFile.Directory.CDN, "${getFileDir()}/$imageFilename");
    }

}
