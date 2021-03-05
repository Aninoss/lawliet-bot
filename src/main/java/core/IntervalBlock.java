package core;

import core.utils.TimeUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class IntervalBlock {

    private final int time;
    private final ChronoUnit chronoUnit;
    Instant nextRequest;

    public IntervalBlock(int time, ChronoUnit chronoUnit) {
        this.time = time;
        this.chronoUnit = chronoUnit;

        nextRequest = Instant.now().plus(time, chronoUnit);
    }

    public boolean block() {
        try {
            long wait = TimeUtil.getMillisBetweenInstants(Instant.now(), nextRequest);
            Thread.sleep(wait);
            nextRequest = Instant.now().plus(time, chronoUnit);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }



}
