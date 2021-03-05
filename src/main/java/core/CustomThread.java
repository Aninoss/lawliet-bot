package core;

public class CustomThread extends Thread {

    public CustomThread(Runnable target, String name) {
        this(target, name, 0);
    }

    public CustomThread(Runnable target, String name, int priority) {
        super(target);
        if (priority > 0) setPriority(priority);
        setName(name);
        setUncaughtExceptionHandler((t1, e) -> MainLogger.get().error("Uncaught Exception", e));
    }

}