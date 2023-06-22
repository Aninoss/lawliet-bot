package core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class ExceptionFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        final IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return FilterReply.NEUTRAL;
        }

        if (!(throwableProxy instanceof ThrowableProxy)) {
            return FilterReply.NEUTRAL;
        }

        final ThrowableProxy throwableProxyImpl = (ThrowableProxy) throwableProxy;
        String message = throwableProxyImpl.getThrowable().toString();
        if (message.contains("java.lang.OutOfMemoryError")) {
            System.err.println("EXIT - Out of Memory (" + Program.getClusterId() + ")");
            throwableProxyImpl.getThrowable().printStackTrace();
            System.exit(1);
            return FilterReply.NEUTRAL;
        }

        return FilterReply.NEUTRAL;
    }

}
