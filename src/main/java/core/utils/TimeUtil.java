package core.utils;

import core.TextManager;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() {}

    public static String getInstantString(Locale locale, Instant instant, boolean withClockTime) {
        String str = DateTimeFormatter
                .ofPattern(TextManager.getString(locale, TextManager.GENERAL, "time_code", withClockTime))
                .localizedBy(locale)
                .format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));

        if (withClockTime) {
            str += " " + TextManager.getString(locale, TextManager.GENERAL, "clock");
        }

        return str;
    }

    public static String getRemainingTimeString(Locale locale, Instant time0, Instant time1, boolean shorter) {
        String remaining = "";

        long diff = Math.abs(Date.from(time0).getTime() - Date.from(time1).getTime()) + 1000 * 60;

        int days = (int) (diff / (24 * 60 * 60 * 1000));
        int hours = (int) (diff / (60 * 60 * 1000) % 24);
        int minutes = (int) (diff / (60 * 1000) % 60);

        String addString = "";
        if (shorter) addString = "_shorter";

        if (days > 0) remaining += days + " " + TextManager.getString(locale, TextManager.GENERAL, "days" + addString, days != 1) + ", ";
        if (hours > 0) remaining += hours + " " + TextManager.getString(locale, TextManager.GENERAL, "hours" + addString, hours != 1) + ", ";
        if (minutes > 0) remaining += minutes + " " + TextManager.getString(locale, TextManager.GENERAL, "minutes" + addString, minutes != 1) + ", ";

        remaining = remaining.substring(0, remaining.length() - 2);
        remaining = StringUtil.replaceLast(remaining, ",", " " + TextManager.getString(locale, TextManager.GENERAL, "and"));
        return remaining;
    }

    public static Instant instantToNextHour(Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime roundCeiling = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        return roundCeiling.toInstant(ZoneOffset.UTC);
    }

    public static Instant instantRoundDownToHour(Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime roundCeiling = now.truncatedTo(ChronoUnit.HOURS);
        return roundCeiling.toInstant(ZoneOffset.UTC);
    }

    public static Instant setInstantToNextDay(Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault());
        LocalDateTime roundCeiling = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        return roundCeiling.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
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
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < 12; i++) {
            if (monthString.equalsIgnoreCase(monthNames[i])) {
                month = i + 1;
                break;
            }
        }

        return month;
    }

    public static Instant parseDateString2(String str) {
        String[] timeString = str.split(" ");
        int month = parseMonth(timeString[2]);

        LocalDateTime ldt1 = LocalDateTime.now()
                .withYear(Integer.parseInt(timeString[3]))
                .withMonth(month)
                .withDayOfMonth(Integer.parseInt(timeString[1]))
                .withHour(Integer.parseInt(timeString[4].split(":")[0]))
                .withMinute(Integer.parseInt(timeString[4].split(":")[1]))
                .withSecond(Integer.parseInt(timeString[4].split(":")[2]));

        return ldt1.atZone(ZoneOffset.UTC).toInstant();
    }

    public static Instant parseDateString3(String str) {
        String timeContent = str.split("\\+")[0];
        int timeOffset = Integer.parseInt(str.split("\\+")[1].split(":")[0]);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.ofHours(timeOffset));
        return formatter.parse(timeContent, Instant::from);
    }

    public static boolean instantHasHour(Instant instant, int hour) {
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
        return calendar.get(Calendar.HOUR_OF_DAY) == hour;
    }

    public static boolean instantHasWeekday(Instant instant, int weekday) {
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
        return calendar.get(Calendar.DAY_OF_WEEK) == weekday;
    }

    public static long getMilisBetweenInstants(Instant instantBefore, Instant instantAfter) {
        Duration duration = Duration.between(instantBefore, instantAfter);
        return Math.max(1, duration.getSeconds() * 1000 + duration.getNano() / 1000000);
    }

    public static Instant localDateToInstant(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

}