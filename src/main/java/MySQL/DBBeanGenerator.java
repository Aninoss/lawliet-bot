package MySQL;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;

public abstract class DBBeanGenerator<T, U extends Observable> implements Observer {

    private final DBBeanGenerator<T, U> instance = this;
    private ArrayList<U> changed;
    private Instant nextCheck;
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

    public DBBeanGenerator() {
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
        tempList.forEach(u -> {
            try {
                saveBean(u);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    protected abstract U loadBean(T t) throws Exception;

    protected abstract void saveBean(U u) throws SQLException;

    public U getBean(T t) throws ExecutionException {
        return cache.get(t);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (this instanceof IntervalSave) {
            U u = (U) o;
            if (!changed.contains(u)) changed.add(u);
        } else {
            try {
                saveBean((U) o);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    protected LoadingCache<T, U> getCache() { return cache; }

    public interface IntervalSave {
        int getIntervalMinutes();
    }

}
