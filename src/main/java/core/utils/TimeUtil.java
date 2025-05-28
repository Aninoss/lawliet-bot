package core.utils;

import core.TextManager;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static String getInstantString(Locale locale, Instant instant, boolean withClockTime) {
        String str = DateTimeFormatter
                .ofPattern(TextManager.getString(locale, TextManager.GENERAL, "time_code", withClockTime))
                .localizedBy(locale)
                .format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));

        if (withClockTime) {
            str += " " + TextManager.getString(locale, TextManager.GENERAL, "clock");
        }

        return str;
    }

    public static String getDurationString(Locale locale, Duration duration) {
        long millis = duration.toMillis();

        int days = (int) (millis / (24 * 60 * 60 * 1000));
        int hours = (int) (millis / (60 * 60 * 1000) % 24);
        int minutes = (int) (millis / (60 * 1000) % 60);

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days)
                    .append(" ")
                    .append(TextManager.getString(locale, TextManager.GENERAL, "days", days != 1))
                    .append(", ");
        }
        if (hours > 0) {
            sb.append(hours)
                    .append(" ")
                    .append(TextManager.getString(locale, TextManager.GENERAL, "hours", hours != 1))
                    .append(", ");
        }
        if (minutes > 0) {
            sb.append(minutes)
                    .append(" ")
                    .append(TextManager.getString(locale, TextManager.GENERAL, "minutes", minutes != 1))
                    .append(", ");
        }

        String durationString = sb.toString();
        if (!durationString.isEmpty()) {
            durationString = durationString.substring(0, durationString.length() - 2);
        }

        durationString = StringUtil.replaceLast(durationString, ",", " " + TextManager.getString(locale, TextManager.GENERAL, "and"));
        return durationString;
    }

    public static String getDurationString(Duration duration) {
        long millis = duration.toMillis();
        int days = (int) (millis / (24 * 60 * 60 * 1000));
        int hours = (int) (millis / (60 * 60 * 1000) % 24);
        int minutes = (int) (millis / (60 * 1000) % 60);

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }

        return sb.toString().trim();
    }

    public static Instant instantToNextMinute(Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime roundCeiling = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        return roundCeiling.toInstant(ZoneOffset.UTC);
    }

    public static Instant instantToNextHour(Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime roundCeiling = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        return roundCeiling.toInstant(ZoneOffset.UTC);
    }

    public static Instant setInstantToNextDay(Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime roundCeiling = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        return roundCeiling.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant setInstantToNextWeek(Instant instant) {
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate nextMonday = date.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return nextMonday.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant parseDateString(String str) {
        String[] timeString = str.split(" ");
        int month = parseMonth(timeString[1]);

        LocalDateTime ldt1 = LocalDateTime.now()
                .withYear(Integer.parseInt(timeString[5]))
                .withMonth(month)
                .withDayOfMonth(Integer.parseInt(timeString[2]))
                .withHour(Integer.parseInt(timeString[3].split(":")[0]))
                .withMinute(Integer.parseInt(timeString[3].split(":")[1]))
                .withSecond(Integer.parseInt(timeString[3].split(":")[2]));

        return ldt1.atZone(ZoneOffset.UTC).toInstant();
    }

    public static int parseMonth(String monthString) {
        int month = -1;
        String[] monthNames = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        for (int i = 0; i < 12; i++) {
            if (monthString.equalsIgnoreCase(monthNames[i])) {
                month = i + 1;
                break;
            }
        }

        return month;
    }

    public static Instant parseDateStringRSS(String str) {
        String[] timeString = str.split(" ");
        int month = parseMonth(timeString[2]);

        LocalDateTime ldt1 = LocalDateTime.now()
                .withYear(Integer.parseInt(timeString[3]))
                .withMonth(month)
                .withDayOfMonth(Integer.parseInt(timeString[1]))
                .withHour(Integer.parseInt(timeString[4].split(":")[0]))
                .withMinute(Integer.parseInt(timeString[4].split(":")[1]))
                .withSecond(Integer.parseInt(timeString[4].split(":")[2]));

        int offset = 0;
        if (StringUtil.stringIsInt(timeString[5])) {
            offset = Integer.parseInt(timeString[5]) / 100;
        }
        return ldt1.atZone(ZoneOffset.ofHours(offset)).toInstant();
    }

    public static boolean instantHasWeekday(Instant instant, int weekday) {
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
        return calendar.get(Calendar.DAY_OF_WEEK) == weekday;
    }

    public static long getMillisBetweenInstants(Instant instantBefore, Instant instantAfter) {
        Duration duration = Duration.between(instantBefore, instantAfter);
        return Math.max(1, duration.getSeconds() * 1000 + duration.getNano() / 1000000);
    }

    public static Instant localDateToInstant(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    public static long currentHour() {
        return System.currentTimeMillis() / 3_600_000L;
    }

    public static int currentHourOfDay() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static int getCurrentWeekOfYear() {
        return LocalDate.now().get(WeekFields.of(Locale.UK).weekOfYear());
    }

    public static int getWeekOfYear(LocalDate localDate) {
        return localDate.get(WeekFields.of(Locale.UK).weekOfYear());
    }

    public static int getCurrentYear() {
        LocalDate localDate = LocalDate.now();
        return localDate.getYear();
    }

}
