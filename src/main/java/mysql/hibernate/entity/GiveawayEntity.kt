package mysql.hibernate.entity

import core.atomicassets.AtomicRole
import mysql.hibernate.InstantConverter
import mysql.hibernate.entity.assets.CdnImageAsset
import mysql.hibernate.entity.assets.NonNullEmojiAsset
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.GenericGenerator
import java.time.Duration
import java.time.Instant
import javax.persistence.*


@Entity(name = "Giveaway")
class GiveawayEntity : HibernateEntity(), HibernateDiscordInterface, CdnImageAsset, NonNullEmojiAsset {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    override var guildId: Long = 0L

    var channelId: Long = 0L

    var messageId: Long = 0L

    var item: String = ""

    var description: String? = null

    var durationMinutes: Int = 10080

    var winners: Int = 1

    override var emojiFormatted: String = "🎉"

    override var imageFilename: String? = null

    @ElementCollection
    var prizeRoleIds = mutableListOf<Long>()
    val prizeRoles: MutableList<AtomicRole>
        get() = getAtomicRoleList(prizeRoleIds)

    @Convert(converter = InstantConverter::class)
    var created: Instant = Instant.MIN
    val end: Instant
        get() = created.plus(Duration.ofMinutes(durationMinutes.toLong()))

    var active: Boolean = true


    fun copy(): GiveawayEntity {
        val copy = GiveawayEntity()
        copy.guildId = guildId
        copy.channelId = channelId
        copy.messageId = messageId
        copy.item = item
        copy.description = description
        copy.durationMinutes = durationMinutes
        copy.winners = winners
        copy.emojiFormatted = emojiFormatted
        copy.imageFilename = imageFilename
        copy.prizeRoleIds = ArrayList(prizeRoleIds)
        copy.created = created
        copy.active = active
        return copy
    }

    override fun getFileDir(): String {
        return "giveaway"
    }

}