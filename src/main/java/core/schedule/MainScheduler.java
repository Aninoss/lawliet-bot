package core.schedule;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import core.AsyncTimer;
import core.MainLogger;
import core.Program;
import core.utils.ExceptionUtil;
import core.utils.TimeUtil;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class MainScheduler {

    private static final MainScheduler ourInstance = new MainScheduler();
    private static final Duration MAX_TASK_DURATION = Duration.ofSeconds(5);

    public static MainScheduler getInstance() {
        return ourInstance;
    }

    private final ScheduledExecutorService schedulers = Executors.newScheduledThreadPool(12, new CountingThreadFactory(() -> "Main", "Scheduler", true));
    private final ScheduledExecutorService pollers = Executors.newScheduledThreadPool(3, new CountingThreadFactory(() -> "Main", "Poller", true));

    public void schedule(long millis, String name, Runnable listener) {
        if (Program.isRunning()) {
            schedulers.schedule(() -> {
                try(AsyncTimer asyncTimer = new AsyncTimer(MAX_TASK_DURATION)) {
                    asyncTimer.setTimeOutListener(t -> {
                        t.interrupt();
                        MainLogger.get().error("Scheduler {} stuck in thread {}", name, t.getName(), ExceptionUtil.generateForStack(t));
                    });
                    listener.run();
                } catch (InterruptedException e) {
                    //ignore
                } catch (Throwable e) {
                    MainLogger.get().error("Unchecked exception in schedule timer", e);
                }
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    public void schedule(long amount, TemporalUnit unit, String name, Runnable listener) {
        long millis = Duration.of(amount, unit).toMillis();
        schedule(millis, name, listener);
    }

    public void schedule(Instant dueInstant, String name, Runnable listener) {
        long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), dueInstant);
        schedule(millis, name, listener);
    }

    /*
    Keeps polling in the specified time interval as long as the listener returns true
     */
    public void poll(long millis, String name, Supplier<Boolean> listener) {
        if (Program.isRunning()) {
            pollers.schedule(() -> {
                try(AsyncTimer asyncTimer = new AsyncTimer(MAX_TASK_DURATION)) {
                    asyncTimer.setTimeOutListener(t -> {
                        t.interrupt();
                        MainLogger.get().error("Scheduler {} stuck in thread {}", name, t.getName(), ExceptionUtil.generateForStack(t));
                    });
                    if (Program.isRunning() && listener.get()) {
                        poll(millis, name, listener);
                    }
                } catch (InterruptedException e) {
                    poll(millis, name, listener);
                } catch (Throwable e) {
                    MainLogger.get().error("Unchecked exception in schedule timer", e);
                }
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    public void poll(long amount, TemporalUnit unit, String name, Supplier<Boolean> listener) {
        long millis = Duration.of(amount, unit).toMillis();
        poll(millis, name, listener);
    }

}
