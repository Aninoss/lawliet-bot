package mysql.hibernate.entity.guild

import core.atomicassets.AtomicMember
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embeddable

const val WORD_FILTER = "wordFilter"

@Embeddable
class WordFilterEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    @Column(name = "$WORD_FILTER.active")
    private var _active: Boolean? = null
    var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    @ElementCollection
    var excludedMemberIds: MutableList<Long> = mutableListOf()
    val excludedMembers: MutableList<AtomicMember>
        get() = getAtomicMemberList(excludedMemberIds)

    @ElementCollection
    var logReceiverUserIds: MutableList<Long> = mutableListOf()
    val logReceivers: MutableList<AtomicMember>
        get() = getAtomicMemberList(logReceiverUserIds)

    @ElementCollection
    var words: MutableList<String> = mutableListOf()


    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
