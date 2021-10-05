package core.lock;

import java.util.LinkedList;

public class Lock implements AutoCloseable {

    private static final LinkedList<Object> locks = new LinkedList<>();
    private final Object lock;

    public Lock(Object lock) throws LockOccupiedException {
        this.lock = lock;
        synchronized (Lock.class) {
            if (locks.contains(lock)) {
                throw new LockOccupiedException("Lock is already being used in another thread");
            } else {
                locks.add(lock);
            }
        }
    }

    @Override
    public void close() {
        synchronized (Lock.class) {
            locks.remove(lock);
        }
    }

}
