package core.collectionadapters;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListAdapter<T, U> implements List<U> {

    private final List<T> list;
    private final Function<T, U> encoderFunction;
    private final Function<U, T> decoderFunction;

    public ListAdapter(List<T> list, Function<T, U> encoderFunction, Function<U, T> decoderFunction) {
        this.list = list;
        this.encoderFunction = encoderFunction;
        this.decoderFunction = decoderFunction;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(decoderFunction.apply((U) o));
    }

    @NotNull
    @Override
    public Iterator<U> iterator() {
        return new IteratorAdapter<>(list.iterator(), encoderFunction);
    }

    @NotNull
    @Override
    public Object[] toArray() {
        Object[] array = list.toArray();
        Object[] encodedArray = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            encodedArray[i] = encoderFunction.apply((T) array[i]);
        }
        return encodedArray;
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return (T1[]) toArray();
    }

    @Override
    public boolean add(U u) {
        return list.add(decoderFunction.apply(u));
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(decoderFunction.apply((U) o));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        Collection<T> collect = decodeCollection((Collection<? extends U>) c);
        return list.containsAll(collect);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends U> c) {
        Collection<T> collect = decodeCollection(c);
        return list.addAll(collect);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends U> c) {
        Collection<T> collect = decodeCollection(c);
        return list.addAll(index, collect);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        Collection<T> collect = decodeCollection((Collection<? extends U>) c);
        return list.removeAll(collect);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        Collection<T> collect = decodeCollection((Collection<? extends U>) c);
        return list.retainAll(collect);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public U get(int index) {
        return encoderFunction.apply(list.get(index));
    }

    @Override
    public U set(int index, U element) {
        T t = list.set(index, decoderFunction.apply((element)));
        return encoderFunction.apply(t);
    }

    @Override
    public void add(int index, U element) {
        list.add(index, decoderFunction.apply(element));
    }

    @Override
    public U remove(int index) {
        return encoderFunction.apply(list.remove(index));
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(decoderFunction.apply((U) o));
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(decoderFunction.apply((U) o));
    }

    @NotNull
    @Override
    public ListIterator<U> listIterator() {
        return new ListIteratorAdapter<>(list.listIterator(), encoderFunction, decoderFunction);
    }

    @NotNull
    @Override
    public ListIterator<U> listIterator(int index) {
        return new ListIteratorAdapter<>(list.listIterator(index), encoderFunction, decoderFunction);
    }

    @NotNull
    @Override
    public List<U> subList(int fromIndex, int toIndex) {
        return encodeCollection(list.subList(fromIndex, toIndex));
    }

    private List<U> encodeCollection(Collection<? extends T> c) {
        return c.stream()
                .map(encoderFunction)
                .collect(Collectors.toList());
    }

    private List<T> decodeCollection(Collection<? extends U> c) {
        return c.stream()
                .map(decoderFunction)
                .collect(Collectors.toList());
    }

}
