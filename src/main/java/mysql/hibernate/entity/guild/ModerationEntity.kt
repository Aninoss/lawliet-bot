package mysql.hibernate.entity.guild

import core.atomicassets.AtomicGuildMessageChannel
import core.atomicassets.AtomicRole
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
    val logChannel: AtomicGuildMessageChannel
        get() = getAtomicGuildMessageChannel(logChannelId)

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
    val banAppealLogChannelEffectively: AtomicGuildMessageChannel
        get() = getAtomicGuildMessageChannel(banAppealLogChannelIdEffectively)

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


    override val guildId: Long
        get() = hibernateEntity.guildId

}
