package core.collectionadapters;

import java.util.ListIterator;
import java.util.function.Function;

public class ListIteratorAdapter<T, U> implements ListIterator<U> {

    private final ListIterator<T> listIterator;
    private final Function<T, U> encoderFunction;
    private final Function<U, T> decoderFunction;

    public ListIteratorAdapter(ListIterator<T> listIterator, Function<T, U> encoderFunction, Function<U, T> decoderFunction) {
        this.listIterator = listIterator;
        this.encoderFunction = encoderFunction;
        this.decoderFunction = decoderFunction;
    }

    @Override
    public boolean hasNext() {
        return listIterator.hasNext();
    }

    @Override
    public U next() {
        return encoderFunction.apply(listIterator.next());
    }

    @Override
    public boolean hasPrevious() {
        return listIterator.hasPrevious();
    }

    @Override
    public U previous() {
        return encoderFunction.apply(listIterator.previous());
    }

    @Override
    public int nextIndex() {
        return listIterator.nextIndex();
    }

    @Override
    public int previousIndex() {
        return listIterator.previousIndex();
    }

    @Override
    public void remove() {
        listIterator.remove();
    }

    @Override
    public void set(U u) {
        listIterator.set(decoderFunction.apply(u));
    }

    @Override
    public void add(U u) {
        listIterator.add(decoderFunction.apply(u));
    }

}
