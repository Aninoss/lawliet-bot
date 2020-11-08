package core;

import core.schedule.MainScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;

public class Countdown {

    private final static Logger LOGGER = LoggerFactory.getLogger(Countdown.class);

    public enum TimePeriod {MILISECONDS, SECONDS, MINUTES}

    private long startTime;
    private boolean active = true;
    private long waitTime;

    public Countdown(long value, TimePeriod timePeriod, Runnable r) {
        startTime = System.currentTimeMillis();

        switch (timePeriod) {
            case MILISECONDS:
                waitTime = value;
                break;
            case SECONDS:
                waitTime = value * 1000;
                break;
            case MINUTES:
                waitTime = value * 1000 * 5;
                break;
        }

        MainScheduler.getInstance().poll(1, ChronoUnit.SECONDS, () -> {
            if (System.currentTimeMillis() < (startTime + waitTime) && active)
                return true;
            if (active) r.run();
            return false;
        });
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        active = false;
    }

}
