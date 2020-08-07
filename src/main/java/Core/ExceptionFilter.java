package Core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Arrays;

public class ExceptionFilter extends Filter<ILoggingEvent> {

    private final String[] FILTERS = {
            "java.net.SocketTimeoutException",
            "org.javacord.api.exception.CannotMessageUserException",
            "java.util.concurrent.RejectedExecutionException",
            "java.lang.InterruptedException",
            "500: Internal Server Error",
            "Read timed out",
            "Unknown Member",
            "disconnect was called already",
            "java.util.concurrent.RejectedExecutionException"
    };

    public ExceptionFilter() {}

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        final IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return FilterReply.NEUTRAL;
        }

        if (!(throwableProxy instanceof ThrowableProxy)) {
            return FilterReply.NEUTRAL;
        }

        final ThrowableProxy throwableProxyImpl =
                (ThrowableProxy) throwableProxy;
        final Throwable throwable = throwableProxyImpl.getThrowable();
        if (Arrays.stream(FILTERS)
                .anyMatch(filter -> throwable.getMessage() != null && throwable.getMessage().contains(filter))
        ) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }

}
