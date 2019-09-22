package General;

import Constants.Settings;

import java.util.Calendar;

public class Countdown {

    public enum TimePeriod { MILISECONDS, SECONDS, MINUTES }
    private long startTime;
    private boolean active = true;
    private long waitTime;

    public Countdown(long value, TimePeriod timePeriod, Runnable r) {
        startTime = System.currentTimeMillis();

        switch(timePeriod) {
            case MILISECONDS: waitTime = value; break;
            case SECONDS: waitTime = value * 1000; break;
            case MINUTES: waitTime = value * 1000 * 5; break;
        }

        new Thread(() -> {
            while(System.currentTimeMillis() < (startTime + waitTime) && active) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (active) r.run();
        }).start();
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        active = false;
    }

}
