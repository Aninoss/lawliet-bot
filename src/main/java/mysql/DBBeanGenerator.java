package mysql;

import core.Bot;
import core.CustomThread;
import core.IntervalBlock;
import core.MainLogger;
import mysql.interfaces.CompleteLoadOnStartup;
import mysql.interfaces.IntervalSave;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import mysql.modules.fisheryusers.DBFishery;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

public abstract class DBBeanGenerator<T, U extends Observable> extends DBCached implements Observer {

    private final DBBeanGenerator<T, U> instance = this;
    private ArrayList<U> changed;
    private boolean allLoaded = false;
    private final LoadingCache<T, U> cache;

    protected CacheBuilder<Object, Object> getCacheBuilder() {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10));
    }

    protected DBBeanGenerator() {
        cache = getCacheBuilder().build(
                new CacheLoader<>() {
                    @Override
                    public U load(@NonNull T t) throws Exception {
                        U u = loadBean(t);
                        u.addObserver(instance);
                        return u;
                    }
                }
        );

        if (this instanceof CompleteLoadOnStartup) {
            getAllBeans();
        }

        if (this instanceof IntervalSave) {
            int minutes = ((IntervalSave)this).getIntervalMinutes();
            changed = new ArrayList<>();

            Runtime.getRuntime().addShutdownHook(new CustomThread(() -> {
                if (changed.size() > 0)
                    intervalSave();
            }, "shutdown_intervalsave"));

            Thread t = new CustomThread(() -> {
                IntervalBlock intervalBlock = new IntervalBlock(Bot.isProductionMode() ? minutes : 1, ChronoUnit.MINUTES);
                while(intervalBlock.block()) {
                    if (this instanceof DBFishery)
                        MainLogger.get().info("Fishery update");
                    if (changed.size() > 0)
                        intervalSave();
                    if (this instanceof DBFishery)
                        MainLogger.get().info("Fishery update completed");
                }
            }, "dbbean_interval_save", 1);
            t.start();
        }
    }

    private synchronized void intervalSave() {
        ArrayList<U> tempList = new ArrayList<>(changed);
        changed = new ArrayList<>();
        tempList.stream()
                .filter(value -> !(value instanceof BeanWithServer) || ((BeanWithServer)value).getServerBean().isSaved())
                .forEach(value -> {
                    try {
                        saveBean(value);
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not save bean", e);
                    }
                });
    }

    protected abstract U loadBean(T t) throws Exception;

    protected abstract void saveBean(U u);

    public U getBean(T t) {
        try {
            return cache.get(t);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected LoadingCache<T, U> getCache() {
        return cache;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (this instanceof IntervalSave) {
            U u = (U) o;
            if (!changed.contains(u)) {
                synchronized (changed) {
                    changed.add(u);
                }
            }
        } else {
            saveBean((U) o);
        }
    }

    protected void removeUpdate(U value) {
        synchronized (changed) {
            changed.remove(value);
        }
    }

    protected void removeUpdateIf(Predicate<U> filter) {
        synchronized (changed) {
            changed.removeIf(filter);
        }
    }

    protected boolean isChanged(U value) {
        synchronized (changed) {
            return changed.contains(value);
        }
    }

    public List<U> getAllBeans() {
        if (this instanceof CompleteLoadOnStartup) {
            if (!allLoaded) {
                try {
                    ((CompleteLoadOnStartup<T>) this).getKeySet().forEach(value -> {
                        try {
                            cache.get(value);
                        } catch (Throwable e) {
                            MainLogger.get().error("Could not fetch cache data", e);
                        }
                    });
                } catch (SQLException throwables) {
                    throw new RuntimeException(throwables);
                }
                allLoaded = true;
            }

            return new ArrayList<>(cache.asMap().values());
        }

        return new ArrayList<>();
    }

    @Override
    public void clear() {
        if (!(this instanceof IntervalSave))
            cache.invalidateAll();
    }

    @Override
    public void autoClear() {
        clear();
    }

}
