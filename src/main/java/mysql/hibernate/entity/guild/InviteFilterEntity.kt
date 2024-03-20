package mysql.hibernate.entity.guild

import core.atomicassets.AtomicGuildChannel
import core.atomicassets.AtomicMember
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.*

const val INVITE_FILTER = "inviteFilter"

@Embeddable
class InviteFilterEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    enum class Action { DELETE_MESSAGE, KICK_USER, BAN_USER }

    @Column(name = "$INVITE_FILTER.active")
    private var _active: Boolean? = null
    var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    @Column(name = "$INVITE_FILTER.action")
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


    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
