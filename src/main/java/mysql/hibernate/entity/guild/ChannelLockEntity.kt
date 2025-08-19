package mysql.hibernate.entity.guild

import mysql.hibernate.InstantConverter
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.GenericGenerator
import java.time.Instant
import javax.persistence.*

@Entity(name = "ChannelLock")
class ChannelLockEntity(override var guildId: Long = 0L, var channelId: Long = 0L) : HibernateEntity(), HibernateDiscordInterface {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    @ElementCollection
    var entityIds = mutableMapOf<Long, Boolean>()

    var modifiedSelfWritePermission: Boolean = true

    @Convert(converter = InstantConverter::class)
    var until: Instant? = null
    var logChannelId: Long = 0L

}