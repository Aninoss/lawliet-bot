package mysql.hibernate.entity

import mysql.hibernate.entity.assets.NullableEmojiAsset
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import org.hibernate.annotations.GenericGenerator
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


@Entity(name = "ReactionRoleSlot")
class ReactionRoleSlotEntity : NullableEmojiAsset {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    override var emojiFormatted: String? = null

    @ElementCollection
    var roleIds: MutableList<Long> = mutableListOf()

    var customLabel: String? = null


    fun getRoles(guild: Guild): List<Role> {
        return roleIds.mapNotNull { guild.getRoleById(it) }
    }

    fun copy(): ReactionRoleSlotEntity {
        val copy = ReactionRoleSlotEntity()
        copy.emojiFormatted = emojiFormatted
        copy.roleIds = ArrayList(roleIds)
        copy.customLabel = customLabel
        return copy
    }

}