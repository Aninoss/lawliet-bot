package core;

import core.schedule.MainScheduler;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AsyncTimer implements AutoCloseable {

    private static final ScheduledExecutorService executorService =
            Executors.newScheduledThreadPool(1, new CountingThreadFactory(() -> "Main", "AsyncTimer", true));

    private boolean pending = true;
    private Consumer<Thread> consumer = null;
    private final Thread thread;

    public AsyncTimer(Duration duration) {
        thread = Thread.currentThread();
        executorService.schedule(() -> {
            if (pending) {
                if (consumer != null) {
                    consumer.accept(thread);
                }
            }
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void setTimeOutListener(Consumer<Thread> consumer) {
        this.consumer = consumer;
    }

    public void interrupt() {
        if (pending) {
            thread.interrupt();
            MainScheduler.poll(Duration.ofMillis(100), () -> {
                if (pending) {
                    thread.interrupt();
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void close() throws InterruptedException {
        pending = false;
    }

}
