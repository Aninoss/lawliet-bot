package core.collectionadapters;

import java.util.Iterator;
import java.util.function.Function;

public class IteratorAdapter<T, U> implements Iterator<U> {

    private final Iterator<T> iterator;
    private final Function<T, U> encoderFunction;

    public IteratorAdapter(Iterator<T> iterator, Function<T, U> encoderFunction) {
        this.iterator = iterator;
        this.encoderFunction = encoderFunction;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public U next() {
        return encoderFunction.apply(iterator.next());
    }

}
