package core.schedule;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.Bot;
import core.utils.ExceptionUtil;
import core.utils.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MainScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(MainScheduler.class);

    private static final MainScheduler ourInstance = new MainScheduler();

    public static MainScheduler getInstance() {
        return ourInstance;
    }

    private final ScheduledExecutorService schedulers = Executors.newScheduledThreadPool(3);
    private final ScheduledExecutorService pollers = Executors.newScheduledThreadPool(2);
    private final Timer timeOutMonitorer = new Timer();

    private final Cache<Long, ScheduleSlot> slotCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    public void schedule(long millis, String name, Runnable listener) {
        if (Bot.isRunning()) {
            ScheduleSlot slot = new ScheduleSlot(name);
            schedulers.schedule(() -> {
                try {
                    slotCache.put(slot.getId(), slot);
                    monitorTimeOuts(slot);
                    listener.run();
                } catch (Throwable e) {
                    LOGGER.error("Unchecked exception in schedule timer");
                }
                slotCache.invalidate(slot.getId());
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    public void schedule(long amount, TemporalUnit unit, String name, Runnable listener) {
        long millis = Duration.of(amount, unit).toMillis();
        schedule(millis, name, listener);
    }

    public void schedule(Instant dueInstant, String name, Runnable listener) {
        long millis = TimeUtil.getMilisBetweenInstants(Instant.now(), dueInstant);
        schedule(millis, name, listener);
    }

    /*
    Keeps polling in the specified time interval as long as the listener returns true
     */
    public void poll(long millis, String name, Supplier<Boolean> listener) {
        if (Bot.isRunning()) {
            ScheduleSlot slot = new ScheduleSlot(name);
            pollers.schedule(() -> {
                try {
                    slotCache.put(slot.getId(), slot);
                    monitorTimeOuts(slot);
                    if (Bot.isRunning() && listener.get()) {
                        poll(millis, name, listener);
                    }
                } catch (Throwable e) {
                    LOGGER.error("Unchecked exception in schedule timer");
                }
                slotCache.invalidate(slot.getId());
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    public void poll(long amount, TemporalUnit unit, String name, Supplier<Boolean> listener) {
        long millis = Duration.of(amount, unit).toMillis();
        poll(millis, name, listener);
    }

    private void monitorTimeOuts(ScheduleSlot slot) {
        Thread runnerThread = Thread.currentThread();
        timeOutMonitorer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (slotCache.asMap().containsKey(slot.getId())) {
                    Exception e = ExceptionUtil.generateForStack(runnerThread);
                    LOGGER.error("Task \"{}\" stuck in scheduler {}", slot.name, runnerThread.getName(), e);
                }
            }
        }, 1000);
    }


    private static class ScheduleSlot {

        private final String name;
        private final long id = System.nanoTime();

        public ScheduleSlot(String name) {
            this.name = name;
        }

        public long getId() {
            return id;
        }

    }

}
