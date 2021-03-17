package mysql;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Predicate;
import core.Program;
import core.CustomThread;
import core.IntervalBlock;
import core.MainLogger;

public abstract class DBIntervalMapCache<T, U extends Observable> extends DBObserverMapCache<T, U> implements Observer {

    private ArrayList<U> changed = new ArrayList<>();

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
                    intervalSave();
                }
            }
        }, "dbbean_interval_save", 1);
        t.start();
    }

    private void intervalSave() {
        ArrayList<U> tempList = new ArrayList<>(changed);
        changed = new ArrayList<>();
        tempList.stream()
                .filter(value -> !(value instanceof BeanWithGuild) || ((BeanWithGuild) value).getGuildBean().isSaved())
                .forEach(value -> {
                    try {
                        save(value);
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not save bean", e);
                    }
                });
    }

    @Override
    public void update(Observable o, Object arg) {
        U u = (U) o;
        if (!changed.contains(u)) {
            synchronized (this) {
                changed.add(u);
            }
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
