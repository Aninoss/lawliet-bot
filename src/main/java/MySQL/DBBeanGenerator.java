package MySQL;

import Core.Bot;
import Core.CustomThread;
import Core.Tools.TimeTools;
import MySQL.Interfaces.CompleteLoadOnStartup;
import MySQL.Interfaces.IntervalSave;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public abstract class DBBeanGenerator<T, U extends Observable> extends DBCached implements Observer {

    final static Logger LOGGER = LoggerFactory.getLogger(DBBeanGenerator.class);

    private final DBBeanGenerator<T, U> instance = this;
    private ArrayList<U> changed;
    private Instant nextCheck;
    private boolean allLoaded = false;
    private final LoadingCache<T, U> cache;

    protected CacheBuilder<Object, Object> getCacheBuilder() {
        return CacheBuilder.newBuilder();
    }

    protected DBBeanGenerator() {
        cache = getCacheBuilder().build(
                new CacheLoader<T, U>() {
                    @Override
                    public U load(@NonNull T t) throws Exception {
                        U u = loadBean(t);
                        u.addObserver(instance);
                        return u;
                    }
                }
        );

        if (this instanceof IntervalSave) {
            int minutes = ((IntervalSave)this).getIntervalMinutes();
            nextCheck = Instant.now().plusSeconds(minutes * 60);
            changed = new ArrayList<>();

            Runtime.getRuntime().addShutdownHook(new CustomThread(() -> {
                if (changed.size() > 0) intervalSave();
            }, "shutdown_intervalsave"));

            Thread t = new CustomThread(() -> {
                try {
                    while(true) {
                        Thread.sleep(TimeTools.getMilisBetweenInstants(Instant.now(), nextCheck));
                        nextCheck = Instant.now().plusSeconds(minutes * 60);
                        if (changed.size() > 0) intervalSave();
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted", e);
                }
            }, "dbbean_interval_save", 1);
            t.start();
        }
    }

    private synchronized void intervalSave() {
        ArrayList<U> tempList = new ArrayList<>(changed);
        changed = new ArrayList<>();
        tempList.stream()
                .filter(value -> !(value instanceof BeanWithServer) || ((BeanWithServer)value).getServerBean().isCached())
                .forEach(value -> {
            try {
                saveBean(value);
            } catch (Throwable e) {
                LOGGER.error("Could not save bean", e);
            }
        });
    }

    protected abstract U loadBean(T t) throws Exception;

    protected abstract void saveBean(U u);

    public U getBean(T t) throws ExecutionException {
        return cache.get(t);
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

    protected boolean isChanged(U value) {
        synchronized (changed) {
            return changed.contains(value);
        }
    }

    public List<U> getAllBeans() throws SQLException {
        if (this instanceof CompleteLoadOnStartup) {
            if (!allLoaded) {
                ((CompleteLoadOnStartup<T>) this).getKeySet().forEach(value -> {
                    try {
                        cache.get(value);
                    } catch (Exception e) {
                        LOGGER.error("Could not fetch cache data", e);
                    }
                });
                allLoaded = true;
            }

            return new ArrayList<>(cache.asMap().values());
        }

        return new ArrayList<>();
    }

    @Override
    public void clear() {
        if ((this instanceof IntervalSave)) intervalSave();
        cache.invalidateAll();
    }

}
