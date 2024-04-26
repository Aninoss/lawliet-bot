package mysql.hibernate.entity.guild

import mysql.hibernate.entity.CustomRolePlayEntity
import mysql.hibernate.entity.assets.CdnImageAsset
import net.dv8tion.jda.api.entities.emoji.Emoji
import javax.persistence.Embeddable

@Embeddable
class CustomCommandEntity: CdnImageAsset {

    var title: String? = null

    var emojiFormatted: String? = null
    val emoji: Emoji?
        get() = if (emojiFormatted != null) Emoji.fromFormatted(emojiFormatted!!) else null

    var textResponse: String = ""

    override var imageFilename: String? = null


    fun copy(): CustomCommandEntity {
        val copy = CustomCommandEntity()
        copy.title = title
        copy.emojiFormatted = emojiFormatted
        copy.textResponse = textResponse
        copy.imageFilename = imageFilename
        return copy
    }

    override fun getFileDir(): String {
        return "custom"
    }

}