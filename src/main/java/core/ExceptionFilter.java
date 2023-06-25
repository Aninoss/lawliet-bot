package core;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import events.sync.SendEvent;

public class ExceptionFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        final IThrowableProxy iThrowableProxy = event.getThrowableProxy();
        final ThrowableProxy throwableProxy = (iThrowableProxy instanceof ThrowableProxy) ? (ThrowableProxy) iThrowableProxy : null;

        if (throwableProxy != null && throwableProxy.getThrowable().toString().contains("java.lang.OutOfMemoryError")) {
            System.err.println("EXIT - Out of Memory (" + Program.getClusterId() + ")");
            throwableProxy.getThrowable().printStackTrace();
            System.exit(1);
            return FilterReply.NEUTRAL;
        }

        if (event.getLevel() == Level.ERROR) {
            String message = throwableProxy != null
                    ? event.getFormattedMessage() + "\n" + extractMessageAndStackTrace(throwableProxy)
                    : event.getFormattedMessage();
            SendEvent.sendException(message);
        }

        return FilterReply.NEUTRAL;
    }

    private String extractMessageAndStackTrace(ThrowableProxy throwableProxy) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
            throwableProxy.getThrowable().printStackTrace(ps);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

}
