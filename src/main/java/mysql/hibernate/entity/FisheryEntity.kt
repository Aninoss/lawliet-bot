package mysql.hibernate.entity

import core.assets.GuildAsset
import modules.fishery.FisheryStatus
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.*

@Embeddable
class FisheryEntity : HibernateEmbeddedEntity<GuildEntity>(), GuildAsset {

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

    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
