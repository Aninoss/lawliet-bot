package mysql;

import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import core.CustomThread;
import core.IntervalBlock;
import core.MainLogger;
import core.Program;
import mysql.modules.fisheryusers.DBFishery;

public abstract class DBIntervalMapCache<T, U extends Observable> extends DBObserverMapCache<T, U> implements Observer {

    private LinkedList<U> changed = new LinkedList<>();

    protected DBIntervalMapCache(int minutes) {
        Runtime.getRuntime().addShutdownHook(new CustomThread(() -> {
            if (changed.size() > 0) {
                intervalSave();
            }
        }, "shutdown_intervalsave"));

        Thread t = new CustomThread(() -> {
            IntervalBlock intervalBlock = new IntervalBlock(Program.isProductionMode() ? minutes : 1, ChronoUnit.MINUTES);
            while (intervalBlock.block()) {
                if (changed.size() > 0) {
                    try {
                        intervalSave();
                    } catch (Throwable e) {
                        MainLogger.get().error("Interval cache exception", e);
                    }
                }
            }
        }, "dbbean_interval_save", 1);
        t.start();
    }

    private void intervalSave() {
        LinkedList<U> tempList = new LinkedList<>(changed);
        changed = new LinkedList<>();
        AtomicInteger saved = new AtomicInteger(0);
        tempList.stream()
                .filter(value -> !(value instanceof BeanWithGuild) || ((BeanWithGuild) value).getGuildBean().isSaved())
                .forEach(value -> {
                    try {
                        save(value);
                        saved.incrementAndGet();
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not save bean", e);
                    }
                });
        if (this instanceof DBFishery) {
            MainLogger.get().info("Fishery DB - {} Actions", saved.get());
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        U u = (U) o;
        if (!changed.contains(u)) {
            changed.add(u);
        }
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
