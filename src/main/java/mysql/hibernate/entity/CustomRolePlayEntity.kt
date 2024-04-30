package mysql.hibernate.entity

import mysql.hibernate.entity.assets.CdnImageListAsset
import mysql.hibernate.entity.assets.NonNullEmojiAsset
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.GenericGenerator
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity(name = "CustomRolePlay")
class CustomRolePlayEntity : HibernateEntity(), CdnImageListAsset, NonNullEmojiAsset {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    var title: String = ""

    override var emojiFormatted: String = ""

    var textNoMembers: String? = null
    var textSingleMember: String? = null
    var textMultiMembers: String? = null
    var nsfw: Boolean = false

    @ElementCollection
    override var imageFilenames: MutableList<String> = mutableListOf()

    fun copy(): CustomRolePlayEntity {
        val copy = CustomRolePlayEntity()
        copy.title = title
        copy.emojiFormatted = emojiFormatted
        copy.textNoMembers = textNoMembers
        copy.textSingleMember = textSingleMember
        copy.textMultiMembers = textMultiMembers
        copy.nsfw = nsfw
        copy.imageFilenames = ArrayList(imageFilenames)
        return copy
    }

    override fun getFileDir(): String {
        return "customrp"
    }

}