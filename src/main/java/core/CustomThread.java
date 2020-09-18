package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomThread extends Thread {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomThread.class);

    public CustomThread(Runnable target, String name) {
        this(target, name, 0);
    }

    public CustomThread(Runnable target, String name, int priority) {
        super(target);
        if (priority > 0) setPriority(priority);
        setName(name);
        setUncaughtExceptionHandler((t1, e) -> LOGGER.error("Uncaught Exception", e));
    }

}