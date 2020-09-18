package core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionEmergencyBreak extends Filter<ILoggingEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExceptionEmergencyBreak.class);
    private Class<?> exceptionClass;

    public ExceptionEmergencyBreak() {}

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
        if (ExceptionHandler.exceptionIsClass(throwable, exceptionClass)) {
            LOGGER.error("EXIT - Emergency break");
            System.exit(-1);
            return FilterReply.NEUTRAL;
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
