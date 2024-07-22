package mysql.hibernate.entity.guild

import core.atomicassets.AtomicGuildMessageChannel
import core.atomicassets.AtomicRole
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embeddable

const val BIRTHDAY = "birthday"

@Embeddable
class BirthdayEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    @Column(name = "$BIRTHDAY.active")
    private var _active: Boolean? = null
    var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    var channelId: Long? = null
    val channel: AtomicGuildMessageChannel
        get() = getAtomicGuildMessageChannel(channelId)

    var roleId: Long? = null
    val role: AtomicRole
        get() = getAtomicRole(roleId)

    @ElementCollection
    val userEntries = mutableMapOf<Long, BirthdayUserEntryEntity>()


    override val guildId: Long
        get() = hibernateEntity.guildId

}
