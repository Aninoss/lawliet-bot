package mysql.hibernate.entity

import mysql.hibernate.InstantConverter
import org.hibernate.annotations.GenericGenerator
import java.time.Instant
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


@Entity(name = "GuildInvite")
class GuildInviteEntity(
    var userId: Long = 0L,
    var uses: Int = 0,
    @Convert(converter = InstantConverter::class) var maxAge: Instant? = null
) {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

}