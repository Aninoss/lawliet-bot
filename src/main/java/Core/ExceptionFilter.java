package Core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class ExceptionFilter extends Filter<ILoggingEvent> {

    private Class<?> exceptionClass;

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
        if (exceptionClass.isInstance(throwable)) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }

    public void setExceptionClassName(final String exceptionClassName) {
        try {
            exceptionClass = Class.forName(exceptionClassName);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Class is unavailable: "
                    + exceptionClassName, e);
        }
    }

}
