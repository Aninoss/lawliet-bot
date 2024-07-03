package mysql.hibernate.entity.guild

import core.atomicassets.AtomicGuildMessageChannel
import mysql.hibernate.entity.InviteTrackingSlotEntity
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.OneToMany

const val INVITE_TRACKING = "inviteTracking"

@Embeddable
class InviteTrackingEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    @Column(name = "$INVITE_TRACKING.active")
    private var _active: Boolean? = null
    var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    var logChannelId: Long? = null
    val logChannel: AtomicGuildMessageChannel
        get() = getAtomicGuildMessageChannel(logChannelId)

    @Column(name = "$INVITE_TRACKING.ping")
    private var _ping: Boolean? = null
    var ping: Boolean
        get() = _ping ?: false
        set(value) {
            _ping = value
        }

    @Column(name = "$INVITE_TRACKING.advanced")
    private var _advanced: Boolean? = null
    var advanced: Boolean
        get() = _advanced ?: true
        set(value) {
            _advanced = value
        }

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @NotFound(action = NotFoundAction.IGNORE)
    var slots = mutableMapOf<Long, InviteTrackingSlotEntity>()


    override val guildId: Long
        get() = hibernateEntity.guildId

    fun removeSlotsOfInviter(inviterUserId: Long) {
        val i: MutableIterator<MutableMap.MutableEntry<Long, InviteTrackingSlotEntity>> = slots.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            if (entry.value.inviterUserId == inviterUserId) {
                i.remove()
            }
        }
    }

    fun isUsed(): Boolean { //TODO: remove after migration
        return _active != null || logChannelId != null || _ping != null || _advanced != null || slots.isNotEmpty()
    }

}
