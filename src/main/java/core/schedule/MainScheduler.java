package core.schedule;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import core.AsyncTimer;
import core.GlobalThreadPool;
import core.MainLogger;
import core.Program;
import core.utils.ExceptionUtil;
import core.utils.TimeUtil;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class MainScheduler {

    private static final Duration MAX_TASK_DURATION = Duration.ofSeconds(5);

    private static final ScheduledExecutorService schedulers = Executors.newScheduledThreadPool(1, new CountingThreadFactory(() -> "Main", "Scheduler", true));
    private static final ScheduledExecutorService pollers = Executors.newScheduledThreadPool(1, new CountingThreadFactory(() -> "Main", "Poller", true));

    public static void schedule(long millis, String name, Runnable listener) {
        if (Program.isRunning()) {
            schedulers.schedule(() -> {
                GlobalThreadPool.getExecutorService().submit(() -> {
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
                });
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    public static void schedule(long amount, TemporalUnit unit, String name, Runnable listener) {
        long millis = Duration.of(amount, unit).toMillis();
        schedule(millis, name, listener);
    }

    public static void schedule(Instant dueInstant, String name, Runnable listener) {
        long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), dueInstant);
        schedule(millis, name, listener);
    }

    /*
    Keeps polling in the specified time interval as long as the listener returns true
     */
    public static void poll(long millis, String name, Supplier<Boolean> listener) {
        if (Program.isRunning()) {
            pollers.schedule(() -> {
                GlobalThreadPool.getExecutorService().submit(() -> {
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
                });
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    public static void poll(long amount, TemporalUnit unit, String name, Supplier<Boolean> listener) {
        long millis = Duration.of(amount, unit).toMillis();
        poll(millis, name, listener);
    }

}
