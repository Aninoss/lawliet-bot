package mysql.hibernate.entity

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.hibernate.annotations.GenericGenerator
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


@Entity(name = "ReactionRoleSlot")
class ReactionRoleSlotEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    var emojiFormatted: String? = null
    val emoji: Emoji?
        get() = if (emojiFormatted != null) Emoji.fromFormatted(emojiFormatted!!) else null

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