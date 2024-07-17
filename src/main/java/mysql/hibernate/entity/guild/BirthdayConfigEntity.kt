package mysql.hibernate.entity.guild

import core.atomicassets.AtomicGuildMessageChannel
import core.atomicassets.AtomicRole
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Column
import javax.persistence.Embeddable

const val BIRTHDAY_CONFIG = "birthdayConfig"

@Embeddable
class BirthdayConfigEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    @Column(name = "$BIRTHDAY_CONFIG.active")
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


    override val guildId: Long
        get() = hibernateEntity.guildId

}
