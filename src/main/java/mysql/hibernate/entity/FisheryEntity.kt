package mysql.hibernate.entity

import core.atomicassets.AtomicTextChannel
import core.cache.ServerPatreonBoostCache
import modules.fishery.FisheryStatus
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import net.dv8tion.jda.api.entities.Role
import java.util.*
import java.util.Objects.requireNonNullElse
import javax.persistence.*

@Embeddable
class FisheryEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    @Enumerated(EnumType.STRING)
    var fisheryStatus: FisheryStatus? = FisheryStatus.STOPPED

    var treasureChests: Boolean? = true
    var powerUps: Boolean? = true
    var fishReminders: Boolean? = true
    var coinGiftLimit: Boolean? = true

    @ElementCollection
    var excludedChannelIds: MutableList<Long> = mutableListOf()
    val excludedChannels: MutableList<AtomicTextChannel>
        get() = getAtomicTextChannelList(excludedChannelIds)

    @ElementCollection
    var roleIds: MutableList<Long> = mutableListOf()
    val roles: List<Role>
        get() {
            val deletedRoleIds = mutableListOf<Long>()
            val roles = getAtomicRoleList(roleIds)
                    .mapNotNull {
                        if (it.get().isEmpty) {
                            deletedRoleIds += it.idLong
                        }
                        return@mapNotNull it.get().orElse(null)
                    }
                    .sortedWith(Comparator.comparingInt { obj: Role -> obj.position })

            if (deletedRoleIds.isNotEmpty()) {
                beginTransaction()
                deletedRoleIds.forEach { roleIds -= it }
                commitTransaction()
            }

            return roles
        }

    var singleRoles: Boolean? = false

    var roleUpgradeChannelId: Long? = null
    val roleUpgradeChannel: AtomicTextChannel
        get() = getAtomicTextChannel(roleUpgradeChannelId)

    var rolePriceMin: Long? = 50_000L
    var rolePriceMax: Long? = 800_000_000L

    var voiceHoursLimit: Int? = 5
        get() = requireNonNullElse(field, 24)
    val voiceHoursLimitEffectively: Int
        get() = if (ServerPatreonBoostCache.get(guildId)) {
            voiceHoursLimit!!
        } else {
            5
        }


    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
