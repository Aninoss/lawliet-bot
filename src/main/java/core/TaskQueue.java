package core;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskQueue {

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public void attach(Runnable r) {
        boolean empty = tasks.size() == 0;
        tasks.offer(r);
        if (empty) {
            new CustomThread(() -> {
                Runnable pollR;
                while((pollR = tasks.poll()) != null)
                    pollR.run();
            }, "task_queue").start();
        }
    }

}
