package mysql.hibernate.entity.guild

import java.time.*
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
        val date = getAdjustedDate(now.year)
        return now.dayOfMonth == date?.dayOfMonth && now.monthValue == date.monthValue && (triggerYear == null || now.year >= triggerYear!!)
    }

    fun updateTriggerYear() {
        triggerYear = Year.now(ZoneId.of(timeZoneEffectively)).value
    }

    fun getNextBirthday(): Instant? {
        if (month == null || day == null) {
            return null
        }

        val zoneId = ZoneId.of(timeZoneEffectively)
        val now = ZonedDateTime.now(ZoneId.of(timeZoneEffectively))
        val date = getAdjustedDate(now.year)!!

        val instant = LocalDate.of(now.year, date.monthValue, date.dayOfMonth).atStartOfDay(zoneId).toInstant()
        if (instant.plus(Duration.ofDays(1)).isBefore(Instant.now())) {
            val newDate = getAdjustedDate(now.year + 1)!!
            return LocalDate.of(now.year + 1, newDate.monthValue, newDate.dayOfMonth).atStartOfDay(zoneId).toInstant()
        } else {
            return instant
        }
    }

    private fun getAdjustedDate(year: Int): MonthDay? {
        if (month == null || day == null) {
            return null
        }

        val moveDateDueToNonLeapYear = day == 29 && month == 2 && YearMonth.of(year, 2).lengthOfMonth() == 28
        val adjustedDay = if (moveDateDueToNonLeapYear) 1 else day!!
        val adjustedMonth = if (moveDateDueToNonLeapYear) 3 else month!!
        return MonthDay.of(adjustedMonth, adjustedDay)
    }

}