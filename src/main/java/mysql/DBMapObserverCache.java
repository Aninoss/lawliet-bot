package mysql;

import java.util.Observable;
import java.util.Observer;

public abstract class DBMapObserverCache<T, U extends Observable> extends DBMapCache<T, U> implements Observer {

    private final DBMapObserverCache<T, U> instance = this;

    @Override
    protected U process(T t) throws Exception {
        U u = DBMapObserverCache.this.load(t);
        u.addObserver(instance);
        return u;
    }

    protected abstract void save(U u);

    @Override
    public void update(Observable o, Object arg) {
        save((U) o);
    }

}
