package MySQL;

import MySQL.Interfaces.CompleteLoadOnStartup;
import MySQL.Interfaces.IntervalSave;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public abstract class DBBeanGenerator<T, U extends Observable> extends DBCached implements Observer {

    private final DBBeanGenerator<T, U> instance = this;
    private ArrayList<U> changed;
    private Instant nextCheck;
    private boolean allLoaded = false;
    private LoadingCache<T, U> cache = CacheBuilder.newBuilder().build(
        new CacheLoader<T, U>() {
            @Override
            public U load(@NonNull T t) throws Exception {
                U u = loadBean(t);
                u.addObserver(instance);
                return u;
            }
        }
    );

    protected DBBeanGenerator() {
        if (this instanceof IntervalSave) {
            int minutes = ((IntervalSave)this).getIntervalMinutes();
            nextCheck = Instant.now().plusSeconds(minutes * 60);
            changed = new ArrayList<>();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (changed.size() > 0) intervalSave();
            }));

            Thread t = new Thread(() -> {
                try {
                    while(true) {
                        Duration duration = Duration.between(Instant.now(), nextCheck);
                        Thread.sleep(Math.max(1, duration.getSeconds() * 1000 + duration.getNano() / 1000000));
                        nextCheck = Instant.now().plusSeconds(minutes * 60);
                        if (changed.size() > 0) intervalSave();
                    }
                } catch (InterruptedException e) {
                    //Ignore
                }
            });
            t.setName("dbbean_interval_save");
            t.setPriority(1);
            t.start();
        }
    }

    private synchronized void intervalSave() {
        ArrayList<U> tempList = new ArrayList<>(changed);
        changed = new ArrayList<>();
        tempList.forEach(this::saveBean);
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
            if (!changed.contains(u)) changed.add(u);
        } else {
            saveBean((U) o);
        }
    }

    public List<U> getAllBeans() throws SQLException {
        if (this instanceof CompleteLoadOnStartup) {
            if (!allLoaded) {
                ((CompleteLoadOnStartup<T>) this).getKeySet().forEach(value -> {
                    try {
                        cache.get(value);
                    } catch (Exception e) {
                        e.printStackTrace();
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
        cache.invalidateAll();
    }

}
