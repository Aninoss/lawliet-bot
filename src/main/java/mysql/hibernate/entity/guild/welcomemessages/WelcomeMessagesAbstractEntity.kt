package mysql.hibernate.entity.guild.welcomemessages

import core.LocalFile
import mysql.hibernate.entity.assets.CdnImageAsset
import mysql.hibernate.entity.assets.CdnImageListAsset
import mysql.hibernate.entity.guild.GuildEntity
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import java.util.concurrent.ThreadLocalRandom
import javax.persistence.Embeddable

@Embeddable
abstract class WelcomeMessagesAbstractEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface, CdnImageAsset, CdnImageListAsset {

    abstract var active: Boolean

    abstract var text: String

    abstract var embeds: Boolean


    override val guildId: Long
        get() = hibernateEntity.guildId

    override fun postLoad() {
        imageFilename?.let {
            imageFilenames += it
            imageFilename = null
        }
    }

    override fun getFileDir(): String {
        return "welcome_images"
    }

    fun retrieveRandomImageUrl(): String? {
        if (imageUrls.isEmpty()) {
            return null
        }
        return imageUrls[ThreadLocalRandom.current().nextInt(imageUrls.size)]
    }

    fun retrieveRandomImageFile(): LocalFile? {
        return retrieveRandomImageUrl()?.let { urlToFile(it) }
    }

}
