package mysql;

import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import core.GlobalThreadPool;
import core.IntervalBlock;
import core.MainLogger;
import core.Program;

public abstract class DBIntervalMapCache<T, U extends Observable> extends DBObserverMapCache<T, U> implements Observer {

    private LinkedList<U> changed = new LinkedList<>();

    protected DBIntervalMapCache(int minutes) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (changed.size() > 0) {
                intervalSave();
            }
        }, "Shutdown DBInterval"));

        Thread t = new Thread(() -> {
            IntervalBlock intervalBlock = new IntervalBlock(Program.productionMode() ? minutes : 1, ChronoUnit.MINUTES);
            while (intervalBlock.block()) {
                if (changed.size() > 0) {
                    try {
                        intervalSave();
                    } catch (Throwable e) {
                        MainLogger.get().error("Interval cache exception", e);
                    }
                }
            }
        }, "DBInterval Save");
        t.start();
    }

    private void intervalSave() {
        LinkedList<U> tempList;
        synchronized (this) {
            tempList = new LinkedList<>(changed);
            changed = new LinkedList<>();
        }

        AtomicInteger saved = new AtomicInteger(0);
        tempList.stream()
                .filter(value -> !(value instanceof DataWithGuild) || ((DataWithGuild) value).getGuildData().isSaved())
                .forEach(value -> {
                    try {
                        save(value);
                        saved.incrementAndGet();
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not save bean", e);
                    }
                });
    }

    @Override
    public void update(Observable o, Object arg) {
        GlobalThreadPool.submit(() -> {
            synchronized (this) {
                U u = (U) o;
                if (!changed.contains(u)) {
                    changed.add(u);
                }
            }
        });
    }

    protected synchronized void removeUpdate(U value) {
        changed.remove(value);
    }

    protected synchronized void removeUpdateIf(Predicate<U> filter) {
        changed.removeIf(filter);
    }

    protected synchronized boolean isChanged(U value) {
        return changed.contains(value);
    }

    @Override
    public void clear() {
    }

}
