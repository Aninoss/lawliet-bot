package mysql.hibernate.entity

import core.atomicassets.AtomicTextChannel
import core.cache.ServerPatreonBoostCache
import modules.fishery.FisheryStatus
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import java.util.*
import javax.persistence.*

@Embeddable
class FisheryEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    @Enumerated(EnumType.STRING)
    var fisheryStatus = FisheryStatus.STOPPED

    var treasureChests = true
    var powerUps = true
    var fishReminders = true
    var coinGiftLimit = true

    @ElementCollection
    val excludedChannels: List<Long> = ArrayList()

    @ElementCollection
    val roles: List<Long> = ArrayList()

    var singleRoles = false
    var roleUpgradeChannelId: Long? = null
    var rolePriceMin = 50_000L
    var rolePriceMax = 800_000_000L
    var voiceHoursLimit: Int? = 5

    val roleUpgradeChannel: AtomicTextChannel
        get() = getAtomicTextChannel(roleUpgradeChannelId)

    val voiceHoursLimitEffectively: Int?
        get() = if (ServerPatreonBoostCache.get(guildId)) {
            voiceHoursLimit
        } else {
            null
        }

    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
