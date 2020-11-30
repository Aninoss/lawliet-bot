package core.schedule;

import core.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class MainScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(MainScheduler.class);

    private static final MainScheduler ourInstance = new MainScheduler();
    public static MainScheduler getInstance() { return ourInstance; }
    private MainScheduler() { }

    private final Timer timer = new Timer();
    private final Timer poller = new Timer();
    private final Timer timeOutMonitorer = new Timer();
    private final ConcurrentLinkedQueue<Thread> callerThreads = new ConcurrentLinkedQueue<>();

    public void schedule(long millis, Runnable listener) {
        if (Bot.isRunning()) {
            Thread t = Thread.currentThread();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        callerThreads.add(t);
                        monitorTimeOuts(t);
                        listener.run();
                    } catch (Throwable e) {
                        LOGGER.error("Unchecked exception in schedule timer");
                    }
                    callerThreads.remove(t);
                }
            }, millis);
        }
    }

    public void schedule(long amount, TemporalUnit unit, Runnable listener) {
        long millis = Duration.of(amount, unit).toMillis();
        schedule(millis, listener);
    }

    /*
    Keeps polling in the specified time interval as long as the listener returns true
     */
    public void poll(long millis, Supplier<Boolean> listener) {
        if (Bot.isRunning()) {
            Thread t = Thread.currentThread();
            poller.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        callerThreads.add(t);
                        monitorTimeOuts(t);
                        if (Bot.isRunning() && listener.get()) {
                            poll(millis, listener);
                        }
                    } catch (Throwable e) {
                        LOGGER.error("Unchecked exception in poll timer");
                    }
                    callerThreads.remove(t);
                }
            }, millis);
        }
    }

    public void poll(long amount, TemporalUnit unit, Supplier<Boolean> listener) {
        long millis = Duration.of(amount, unit).toMillis();
        poll(millis, listener);
    }

    private void monitorTimeOuts(Thread callerThread) {
        timeOutMonitorer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (callerThreads.contains(callerThread)) {
                    LOGGER.warn("Task {} stuck in scheduler", callerThread.getName());
                }
            }
        }, 500);
    }

}
