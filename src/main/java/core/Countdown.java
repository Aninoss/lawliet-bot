package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        Thread t = new CustomThread(() -> {
            try {
                while (System.currentTimeMillis() < (startTime + waitTime) && active) {
                    Thread.sleep(1000);
                }
                if (active) r.run();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        }, "countdown_processor", 1);
        t.start();
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        active = false;
    }

}
