package mysql.hibernate.entity.guild

import javax.persistence.Embeddable

@Embeddable
class AutoModEntity(
        var infractions: Int? = null,
        var infractionDays: Int? = null,
        var durationMinutes: Int? = null
)