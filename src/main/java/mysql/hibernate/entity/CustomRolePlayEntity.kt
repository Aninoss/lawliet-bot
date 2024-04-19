package mysql.hibernate.entity

import core.LocalFile
import mysql.hibernate.template.HibernateEntity
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.hibernate.annotations.GenericGenerator
import java.io.File
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity(name = "CustomRolePlay")
class CustomRolePlayEntity : HibernateEntity() {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    var title: String = ""

    var emojiFormatted: String = ""
    val emoji: Emoji
        get() = Emoji.fromFormatted(emojiFormatted)

    var textNoMembers: String? = null
    var textSingleMember: String? = null
    var textMultiMembers: String? = null
    var nsfw: Boolean = false

    @ElementCollection
    var imageAttachments: MutableList<String> = mutableListOf()
    val imageAttachmentFiles: List<File>
        get() = imageAttachments
                .map { LocalFile(LocalFile.Directory.CDN, "customrp/${it}") }
    val imageAttachmentUrls: List<String>
        get() = imageAttachments
                .map { "https://lawlietbot.xyz/cdn/customrp/$it" }

    fun copy(): CustomRolePlayEntity {
        val copy = CustomRolePlayEntity()
        copy.title = title
        copy.emojiFormatted = emojiFormatted
        copy.textNoMembers = textNoMembers
        copy.textSingleMember = textSingleMember
        copy.textMultiMembers = textMultiMembers
        copy.nsfw = nsfw
        copy.imageAttachments = ArrayList(imageAttachments)
        return copy
    }

}