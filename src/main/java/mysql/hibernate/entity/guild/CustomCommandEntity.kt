package mysql.hibernate.entity.guild

import mysql.hibernate.entity.CustomRolePlayEntity
import net.dv8tion.jda.api.entities.emoji.Emoji
import javax.persistence.Embeddable

@Embeddable
class CustomCommandEntity {

    var title: String? = null

    var emojiFormatted: String? = null
    val emoji: Emoji?
        get() = if (emojiFormatted != null) Emoji.fromFormatted(emojiFormatted!!) else null

    var textResponse: String = ""

    var imageFilename: String? = null
    var imageUrl: String?
        get() = if (imageFilename != null) "https://lawlietbot.xyz/cdn/custom/$imageFilename" else null
        set(value) {
            if (value == null || value.isEmpty()) {
                imageFilename = null
            } else {
                imageFilename = value.split("/")[5]
            }
        }


    fun copy(): CustomCommandEntity {
        val copy = CustomCommandEntity()
        copy.title = title
        copy.emojiFormatted = emojiFormatted
        copy.textResponse = textResponse
        copy.imageFilename = imageFilename
        return copy
    }

}