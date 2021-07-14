package core;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class AsyncTimer implements AutoCloseable {

    private static final ScheduledExecutorService executorService =
            Executors.newScheduledThreadPool(1, new CountingThreadFactory(() -> "Main", "AsyncTimer", true));

    private boolean pending = true;
    private Consumer<Thread> consumer = null;

    public AsyncTimer(Duration duration) {
        Thread t = Thread.currentThread();
        executorService.schedule(() -> {
            if (pending) {
                if (consumer != null) {
                    consumer.accept(t);
                }
            }
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void setTimeOutListener(Consumer<Thread> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void close() throws InterruptedException {
        pending = false;
    }

}
