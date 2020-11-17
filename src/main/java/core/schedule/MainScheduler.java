package core.schedule;

import core.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Timer;
import java.util.TimerTask;

public class MainScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(MainScheduler.class);

    private static final MainScheduler ourInstance = new MainScheduler();
    public static MainScheduler getInstance() { return ourInstance; }
    private MainScheduler() { }

    private final Timer timer = new Timer();
    private final Timer poller = new Timer();

    public void schedule(long millis, Runnable listener) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    listener.run();
                } catch (Throwable e) {
                    LOGGER.error("Unchecked exception in schedule timer");
                }
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
        poller.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (Bot.isRunning() && listener.run()) {
                        poll(millis, listener);
                    }
                } catch (Throwable e) {
                    LOGGER.error("Unchecked exception in poll timer");
                }
            }
        }, millis);
    }

    public void poll(long amount, TemporalUnit unit, RunnableWithBoolean listener) {
        long millis = Duration.of(amount, unit).toMillis();
        poll(millis, listener);
    }


    public interface RunnableWithBoolean {

        boolean run();

    }

}
