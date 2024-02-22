package mysql.hibernate.entity.guild

import javax.persistence.Embeddable

@Embeddable
class BanAppealEntity(
        var userId: Long = 0L,
        var text: String = "",
        var open: Boolean = true
)