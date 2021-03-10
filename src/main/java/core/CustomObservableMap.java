package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Function;
import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.collections.MapChangeListener;

public class CustomObservableMap<T, U> extends ObservableMapWrapper<T, U> implements Observer {

    private final ArrayList<MapAddListener<T, U>> mapAddListeners = new ArrayList<>();
    private final ArrayList<MapRemoveListener<T, U>> mapRemoveListeners = new ArrayList<>();
    private final ArrayList<MapUpdateListener<T, U>> mapUpdateListeners = new ArrayList<>();

    public CustomObservableMap(HashMap<T, U> map) {
        super(map);
        map.values().forEach(value -> {
            if (value instanceof Observable) ((Observable) value).addObserver(this);
        });

        addListener((MapChangeListener<? super T, ? super U>) this::onChange);
    }

    private void onChange(MapChangeListener.Change<? extends T, ? extends U> c) {
        if (c.wasAdded()) {
            U value = c.getValueAdded();
            if (value instanceof Observable) ((Observable) value).addObserver(this);
            mapAddListeners.forEach(mapAddListener -> mapAddListener.onMapAdd(value));
        } else if (c.wasRemoved()) {
            mapRemoveListeners.forEach(mapRemoveListeners -> mapRemoveListeners.onMapRemove(c.getValueRemoved()));
        }
    }

    public CustomObservableMap<T, U> addMapAddListener(MapAddListener<T, U> mapAddListener) {
        if (!mapAddListeners.contains(mapAddListener)) mapAddListeners.add(mapAddListener);
        return this;
    }

    public CustomObservableMap<T, U> addMapRemoveListener(MapRemoveListener<T, U> mapRemoveListener) {
        if (!mapRemoveListeners.contains(mapRemoveListener)) mapRemoveListeners.add(mapRemoveListener);
        return this;
    }

    public CustomObservableMap<T, U> addMapUpdateListener(MapUpdateListener<T, U> mapUpdateListener) {
        if (!mapUpdateListeners.contains(mapUpdateListener)) mapUpdateListeners.add(mapUpdateListener);
        return this;
    }

    @Override
    public void update(Observable o, Object arg) {
        mapUpdateListeners.forEach(mapUpdateListener -> mapUpdateListener.onMapUpdate((U) o));
    }

    public interface MapAddListener<T, U> {

        void onMapAdd(U value);

    }

    public interface MapRemoveListener<T, U> {

        void onMapRemove(U value);

    }

    public interface MapUpdateListener<T, U> {

        void onMapUpdate(U value);

    }

    public <V> void trim(Function<T, V> function) {
        for (T key : new HashMap<>(this).keySet()) {
            V opt = function.apply(key);
            if (opt != null) {
                remove(key);
            }
        }
    }

}
