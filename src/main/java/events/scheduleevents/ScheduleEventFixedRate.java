package events.scheduleevents;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.temporal.ChronoUnit;

@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduleEventFixedRate {

    long rateValue();

    ChronoUnit rateUnit();

}