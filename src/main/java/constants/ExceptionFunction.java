package constants;

public interface ExceptionFunction<T, R> {

    R apply(T t) throws Throwable;

}
