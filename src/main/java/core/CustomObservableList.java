package core;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ListChangeListener;

import java.util.*;
import java.util.function.Function;

public class CustomObservableList<T> extends ObservableListWrapper<T> implements Observer {

    private final ArrayList<ListAddListener<T>> listAddListeners = new ArrayList<>();
    private final ArrayList<ListRemoveListener<T>> listRemoveListeners = new ArrayList<>();
    private final ArrayList<ListUpdateListener<T>> listUpdateListeners = new ArrayList<>();

    public CustomObservableList(ArrayList<T> list) {
        super(list);
        list.forEach(value -> {
            if (value instanceof Observable) ((Observable) value).addObserver(this);
        });
        addListener((ListChangeListener<? super T>) this::onChange);
    }

    private void onChange(ListChangeListener.Change<? extends T> c) {
        while(c.next()) {
            if (c.wasAdded()) {
                c.getAddedSubList().forEach(t -> {
                    if (t instanceof Observable) ((Observable) t).addObserver(this);
                });
                listAddListeners.forEach(listAddListener -> listAddListener.onListAdd(c.getAddedSubList()));
            } else if (c.wasRemoved())
                listRemoveListeners.forEach(listRemoveListener -> listRemoveListener.onListRemove(c.getRemoved()));
        }
    }

    public CustomObservableList<T> addListAddListener(ListAddListener<T> listAddListener) {
        if (!listAddListeners.contains(listAddListener)) listAddListeners.add(listAddListener);
        return this;
    }

    public CustomObservableList<T> addListRemoveListener(ListRemoveListener<T> listRemoveListener) {
        if (!listRemoveListeners.contains(listRemoveListener)) listRemoveListeners.add(listRemoveListener);
        return this;
    }

    public CustomObservableList<T> addListUpdateListener(ListUpdateListener<T> listUpdateListener) {
        if (!listUpdateListeners.contains(listUpdateListener)) listUpdateListeners.add(listUpdateListener);
        return this;
    }

    @Override
    public void update(Observable o, Object arg) {
        listUpdateListeners.forEach(listUpdateListener -> listUpdateListener.onListUpdate((T)o));
    }

    public interface ListAddListener<T> { void onListAdd(List<? extends T> list); }
    public interface ListRemoveListener<T> { void onListRemove(List<? extends T> list); }
    public interface ListUpdateListener<T> { void onListUpdate(T t); }

    public <U> CustomObservableList<U> transform(Function<T, Optional<U>> function, Function<U, T> backFunction) {
        ArrayList<U> listTemp = new ArrayList<>();

        for(T t: new ArrayList<>(this)) {
            Optional<U> opt = function.apply(t);
            if (opt.isPresent()) {
                listTemp.add(opt.get());
            } else {
                remove(t);
            }
        }

        CustomObservableList<U> newList = new CustomObservableList<>(listTemp);
        newList.addListAddListener(addedList -> addedList.forEach(element -> add(backFunction.apply(element))));
        newList.addListRemoveListener(removedList -> removedList.forEach(element -> remove(backFunction.apply(element))));

        return newList;
    }

}
