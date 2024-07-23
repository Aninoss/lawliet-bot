package mysql.hibernate.entity.guild

import java.time.Year
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.Embeddable

@Embeddable
class BirthdayUserEntryEntity {

    var day: Int? = null
    var month: Int? = null

    var timeZone: String? = null
    val timeZoneEffectively: String
        get() = timeZone ?: "GMT"

    var triggerYear: Int? = null
    var triggered: Boolean? = null

    fun isBirthday(): Boolean {
        val now = ZonedDateTime.now(ZoneId.of(timeZone))
        return now.dayOfMonth == day && now.month.value == month && (triggerYear == null || now.year >= triggerYear!!)
    }

    fun updateTriggerYear() {
        triggerYear = Year.now(ZoneId.of(timeZone)).value + 1
    }

}