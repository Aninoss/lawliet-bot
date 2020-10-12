package mysql.interfaces;

public interface SQLFunction<T, R> {

    R apply(T t) throws Throwable;

}
