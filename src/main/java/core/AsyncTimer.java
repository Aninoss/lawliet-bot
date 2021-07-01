package core;

import java.time.Duration;
import java.util.function.Consumer;
import core.schedule.MainScheduler;

public class AsyncTimer implements AutoCloseable {

    private boolean pending = true;
    private Consumer<Thread> consumer = null;

    public AsyncTimer(Duration duration) {
        Thread t = Thread.currentThread();
        MainScheduler.getInstance().schedule(duration.toMillis(), "timed_interruptor", () -> {
            if (pending) {
                if (consumer != null) {
                    consumer.accept(t);
                }
            }
        });
    }

    public void setTimeOutListener(Consumer<Thread> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void close() throws Exception {
        pending = false;
    }

}
