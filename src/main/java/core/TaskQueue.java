package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskQueue {

    private final static Logger LOGGER = LoggerFactory.getLogger(TaskQueue.class);

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final ArrayList<Runnable> completedListeners = new ArrayList<>();

    public void attach(Runnable r) {
        boolean empty = tasks.size() == 0;
        tasks.offer(r);
        if (empty) {
            new CustomThread(() -> {
                Runnable pollR;
                while((pollR = tasks.poll()) != null) {
                    try {
                        pollR.run();
                    } catch (Throwable e) {
                        LOGGER.error("Error in task queue", e);
                    }
                }
                completedListeners.forEach(Runnable::run);
            }, "task_queue").start();
        }
    }

    public void addQueueCompletedListener(Runnable r) {
        completedListeners.add(r);
    }

    public boolean removeQueueCompletedListener(Runnable r) {
        return completedListeners.remove(r);
    }

}
