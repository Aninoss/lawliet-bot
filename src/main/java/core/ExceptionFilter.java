package core;

import java.util.Arrays;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class ExceptionFilter extends Filter<ILoggingEvent> {

    private final String[] FILTERS = {
            "10008",    /* Unknown message */
            "50007",    /* Cannot send messages to this user */
            "The Requester has been stopped! No new requests can be requested!"
    };

    public ExceptionFilter() {
    }

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
        if (!shouldBeVisible(throwableProxyImpl.getThrowable().toString()) || !shouldBeVisible(event.getFormattedMessage())) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }

    public boolean shouldBeVisible(String message) {
        return !Program.isProductionMode() || Arrays.stream(FILTERS).noneMatch(message::contains);
    }

}
