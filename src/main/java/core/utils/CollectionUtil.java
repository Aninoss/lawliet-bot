package core.utils;

import java.util.Collection;

public class CollectionUtil {

    public static <T> void replace(Collection<T> oldCollection, Collection<T> newCollection) {
        oldCollection.clear();
        oldCollection.addAll(newCollection);
    }

}
