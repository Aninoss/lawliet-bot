package mysql.hibernate.entity.guild

import mysql.hibernate.entity.assets.CdnImageAsset
import mysql.hibernate.entity.assets.NullableEmojiAsset
import javax.persistence.Embeddable

@Embeddable
class CustomCommandEntity: CdnImageAsset, NullableEmojiAsset {

    var title: String? = null

    override var emojiFormatted: String? = null

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