package core.schedule;

import core.GlobalThreadPool;
import core.MainLogger;
import core.utils.TimeUtil;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MainScheduler {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory(() -> "Main", "Scheduler", true));

    public static ScheduledFuture<?> schedule(Duration duration, Runnable command) {
        return scheduler.schedule(() -> {
            GlobalThreadPool.submit(() -> {
                try {
                    command.run();
                } catch (Throwable e) {
                    MainLogger.get().error("Unchecked exception in scheduler", e);
                }
            });
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture<?> schedule(Instant dueInstant, Runnable command) {
        long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), dueInstant);
        return schedule(Duration.ofMillis(millis), command);
    }

    /* keeps polling in the specified time interval as long as the listener returns true */
    public static void poll(Duration duration, Supplier<Boolean> command) {
        scheduler.schedule(() -> {
            GlobalThreadPool.submit(() -> {
                try {
                    if (command.get()) {
                        poll(duration, command);
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Unchecked exception in scheduler", e);
                }
            });
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

}
