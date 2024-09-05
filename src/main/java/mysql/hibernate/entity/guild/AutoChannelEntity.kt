package mysql.hibernate.entity.guild

import core.atomicassets.AtomicVoiceChannel
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embeddable

const val AUTO_CHANNEL = "autoChannel"

@Embeddable
class AutoChannelEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    @Column(name = "$AUTO_CHANNEL.active")
    private var _active: Boolean? = null
    var active: Boolean
        get() = _active ?: false
        set(value) {
            _active = value
        }

    @Column(name = "$AUTO_CHANNEL.nameMask")
    private var _nameMask: String? = null
    var nameMask: String
        get() = _nameMask ?: "%VCName [%Creator]"
        set(value) {
            _nameMask = value
        }

    @ElementCollection
    var parentChannelIds: MutableList<Long> = mutableListOf()
    val parentChannels: MutableList<AtomicVoiceChannel>
        get() = getAtomicVoiceChannelList(parentChannelIds)

    @Column(name = "$AUTO_CHANNEL.beginLocked")
    private var _beginLocked: Boolean? = null
    var beginLocked: Boolean
        get() = _beginLocked ?: false
        set(value) {
            _beginLocked = value
        }

    @ElementCollection
    var childChannelIdsToParentChannelId: MutableMap<Long, Long> = mutableMapOf()


    override val guildId: Long
        get() = hibernateEntity.guildId

    fun isUsed(): Boolean { //TODO: remove after migration
        return _active != null || _nameMask != null || parentChannelIds.isNotEmpty() || _beginLocked != null || childChannelIdsToParentChannelId.isNotEmpty()
    }

}
