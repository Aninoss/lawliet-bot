package core;

import java.util.Arrays;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionLogger {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExceptionLogger.class);

    public static <T> Function<Throwable, T> get(String... exceptions) {
        return throwable -> {
            if (Arrays.stream(exceptions).noneMatch(exc -> throwable.toString().toLowerCase().contains(exc.toLowerCase()))) {
                LOGGER.error("Exception", throwable);
            }
            return null;
        };
    }

}
