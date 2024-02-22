package mysql.hibernate.entity.guild

import javax.persistence.Embeddable

@Embeddable
class BanAppealEntity(
        var message: String = "",
        var open: Boolean = true
)