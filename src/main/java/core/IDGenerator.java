package core;

import java.time.Instant;

public class IDGenerator {

    private static final IDGenerator ourInstance = new IDGenerator();
    public static IDGenerator getInstance() { return ourInstance; }
    private IDGenerator() { }

    private int counter = 0;

    public synchronized long getId() {
        return (Instant.now().getEpochSecond() << 8) + (++counter);
    }

}
