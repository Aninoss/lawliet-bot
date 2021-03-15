package mysql;

import java.util.Observable;
import java.util.Observer;

public abstract class DBObserverMapCache<T, U extends Observable> extends DBMapCache<T, U> implements Observer {

    private final DBObserverMapCache<T, U> instance = this;

    protected DBObserverMapCache() {
    }

    @Override
    protected U process(T t) throws Exception {
        U u = DBObserverMapCache.this.load(t);
        u.addObserver(instance);
        return u;
    }

    protected abstract void save(U u);

    @Override
    public void update(Observable o, Object arg) {
        save((U) o);
    }

}
