package mysql.hibernate.entity.guild

import java.time.Year
import java.time.YearMonth
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
        val now = ZonedDateTime.now(ZoneId.of(timeZoneEffectively))
        val moveDateDueToNonLeapYear = day == 29 && month == 2 && YearMonth.of(now.year, 2).lengthOfMonth() == 28
        val adjustedDay = if (moveDateDueToNonLeapYear) 1 else day
        val adjustedMonth = if (moveDateDueToNonLeapYear) 3 else month

        return now.dayOfMonth == adjustedDay && now.month.value == adjustedMonth && (triggerYear == null || now.year >= triggerYear!!)
    }

    fun updateTriggerYear() {
        triggerYear = Year.now(ZoneId.of(timeZoneEffectively)).value
    }

}