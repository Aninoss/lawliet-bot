package mysql.hibernate.entity.guild

import core.atomicassets.AtomicRole
import core.atomicassets.AtomicTextChannel
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embeddable

const val MODERATION = "moderation"

@Embeddable
class ModerationEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    var logChannelId: Long? = null
    val logChannel: AtomicTextChannel
        get() = getAtomicTextChannel(logChannelId)

    @Column(name = "$MODERATION.confirmationMessages")
    private var _confirmationMessages: Boolean? = null
    var confirmationMessages: Boolean
        get() = _confirmationMessages ?: true
        set(value) {
            _confirmationMessages = value
        }

    var banAppealLogChannelId: Long? = null
    val banAppealLogChannel: AtomicTextChannel
        get() = getAtomicTextChannel(banAppealLogChannelId)

    @ElementCollection
    var jailRoleIds: MutableList<Long> = mutableListOf()
    val jailRoles: MutableList<AtomicRole>
        get() = getAtomicRoleList(jailRoleIds)

    val autoMute = AutoModEntity()
    val autoJail = AutoModEntity()
    val autoKick = AutoModEntity()
    val autoBan = AutoModEntity()


    fun isUsed(): Boolean { //TODO: remove afterwards
        return _confirmationMessages != null
    }

    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
