package core.schedule;

import core.Bot;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Timer;
import java.util.TimerTask;

public class MainScheduler {

    private static final MainScheduler ourInstance = new MainScheduler();
    public static MainScheduler getInstance() { return ourInstance; }
    private MainScheduler() { }

    private final Timer timer = new Timer();

    public void schedule(long millis, Runnable listener) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                listener.run();
            }
        }, millis);
    }

    public void schedule(long amount, TemporalUnit unit, Runnable listener) {
        long millis = Duration.of(amount, unit).toMillis();
        schedule(millis, listener);
    }

    /*
    Keeps polling in the specified time interval as long as the listener returns true
     */
    public void poll(long millis, RunnableWithBoolean listener) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (Bot.isRunning() && listener.run()) {
                    poll(millis, listener);
                }
            }
        }, millis);
    }

    public void poll(long amount, TemporalUnit unit, RunnableWithBoolean listener) {
        long millis = Duration.of(amount, unit).toMillis();
        poll(millis, listener);
    }


    public static interface RunnableWithBoolean {

        public boolean run();

    }

}
