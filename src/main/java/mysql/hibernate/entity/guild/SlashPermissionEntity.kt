package mysql.hibernate.entity.guild

import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
class SlashPermissionEntity {

    enum class Type { ROLE, USER, CHANNEL, UNKNOWN }

    var command: String = ""
    var objectId: Long = 0L

    @Enumerated(EnumType.STRING)
    var type: Type = Type.UNKNOWN

    var enabled = false

    fun isDefaultObject(guildId: Long): Boolean {
        return (type == Type.ROLE || type == Type.CHANNEL) && objectId <= guildId
    }

}