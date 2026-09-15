package mysql.hibernate.entity.guild

import core.atomicassets.AtomicGuildChannel
import core.atomicassets.AtomicMember
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

const val SPAM_FILTER = "spamFilter"

@Embeddable
class SpamFilterEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    enum class Action { DELETE_MESSAGE, KICK_USER, BAN_USER }

    @Column(name = "$SPAM_FILTER.active")
    private var _active: Boolean? = null
    var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    @Column(name = "$SPAM_FILTER.action")
    @Enumerated(EnumType.STRING)
    private var _action: Action? = null
    var action: Action
        get() = _action ?: Action.DELETE_MESSAGE
        set(value) {
            _action = value
        }

    @ElementCollection
    var excludedMemberIds: MutableList<Long> = mutableListOf()
    val excludedMembers: MutableList<AtomicMember>
        get() = getAtomicMemberList(excludedMemberIds)

    @ElementCollection
    var excludedChannelIds: MutableList<Long> = mutableListOf()
    val excludedChannels: MutableList<AtomicGuildChannel>
        get() = getAtomicGuildChannelList(excludedChannelIds)

    @ElementCollection
    var logReceiverUserIds: MutableList<Long> = mutableListOf()
    val logReceivers: MutableList<AtomicMember>
        get() = getAtomicMemberList(logReceiverUserIds)


    override val guildId: Long
        get() = hibernateEntity.guildId

}
