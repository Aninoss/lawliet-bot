package core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CollectionUtil {

    public static <T> void replace(Collection<T> oldCollection, Collection<T> newCollection) {
        for (T t : new ArrayList<>(oldCollection)) {
            if (!newCollection.contains(t)) {
                oldCollection.remove(t);
            }
        }
        for (T newItem : newCollection) {
            if (!oldCollection.contains(newItem)) {
                oldCollection.add(newItem);
            }
        }
    }

    public static <T> T[] arrayConcat(T[] array1, T[] array2) {
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static <T> T[] arrayConcat(T[] array1, T value) {
        Object[] array2 = new Object[]{value};
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

}
