package General;

import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.collections.MapChangeListener;
import java.util.*;
import java.util.function.Function;

public class CustomObservableMap<T, U> extends ObservableMapWrapper<T, U> implements Observer {

    private ArrayList<MapAddListener<T, U>> mapAddListeners = new ArrayList<>();
    private ArrayList<MapRemoveListener<T, U>> mapRemoveListeners = new ArrayList<>();
    private ArrayList<MapUpdateListener<T, U>> mapUpdateListeners = new ArrayList<>();

    public CustomObservableMap(HashMap<T, U> map) {
        super(map);
        map.values().forEach(value -> {
            if (value instanceof Observable) ((Observable) value).addObserver(this);
        });
        addListener((MapChangeListener<? super T, ? super U>) this::onChange);
    }

    private void onChange(MapChangeListener.Change<? extends T,? extends U> c) {
        if (c.wasAdded()) {
            U value = c.getValueAdded();
            if (value instanceof Observable) ((Observable) value).addObserver(this);
            mapAddListeners.forEach(mapAddListener -> mapAddListener.onMapAdd(value));
        } else if (c.wasRemoved())
            mapRemoveListeners.forEach(mapRemoveListeners -> mapRemoveListeners.onMapRemove(c.getValueRemoved()));
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
        mapUpdateListeners.forEach(mapUpdateListener -> mapUpdateListener.onMapUpdate((U)o));
    }

    public interface MapAddListener<T, U> { void onMapAdd(U value); }
    public interface MapRemoveListener<T, U> { void onMapRemove(U value); }
    public interface MapUpdateListener<T, U> { void onMapUpdate(U value); }

    public <V> Map<T, V> transform(Function<T, Optional<V>> function) {
        HashMap<T, V> newMap = new HashMap<>();

        for(T key: new HashMap<>(this).keySet()) {
            Optional<V> opt = function.apply(key);
            if (opt.isPresent()) {
                newMap.put(key, opt.get());
            } else {
                remove(key);
            }
        }

        return newMap;
    }

}
