package mysql.hibernate.entity.guild

import core.atomicassets.AtomicRole
import core.atomicassets.AtomicTextChannel
import core.cache.ServerPatreonBoostCache
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
    val banAppealLogChannelIdEffectively: Long?
        get() = if (ServerPatreonBoostCache.get(guildId)) banAppealLogChannelId else null
    val banAppealLogChannelEffectively: AtomicTextChannel
        get() = getAtomicTextChannel(banAppealLogChannelIdEffectively)

    @ElementCollection
    var jailRoleIds: MutableList<Long> = mutableListOf()
    val jailRoles: MutableList<AtomicRole>
        get() = getAtomicRoleList(jailRoleIds)

    val autoMute = AutoModEntity()
    val autoJail = AutoModEntity()
    val autoKick = AutoModEntity()
    val autoBan = AutoModEntity()

    @ElementCollection
    val banAppeals = mutableMapOf<Long, BanAppealEntity>()


    fun isUsed(): Boolean { //TODO: remove afterwards
        return _confirmationMessages != null
    }

    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
