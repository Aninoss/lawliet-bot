package mysql.hibernate.entity.guild

import javax.persistence.Embeddable

@Embeddable
class BirthdayUserEntryEntity {

    var day: Int? = null
    var month: Int? = null

    var timeZone: String? = null
    val timeZoneEffectively: String
        get() = timeZone ?: "GMT"

    var triggeredYear: Int? = null

}