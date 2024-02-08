package core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import events.sync.SendEvent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public class ExceptionFilter extends Filter<ILoggingEvent> {

    public static int MAX_ERRORS_PER_MINUTE = Integer.parseInt(Objects.requireNonNullElse(System.getenv("MAX_ERRORS_PER_MINUTE"), "30"));

    private final RatelimitManager ratelimitManager = new RatelimitManager();

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

        String message = throwableProxy != null
                ? event.getFormattedMessage() + "\n" + extractMessageAndStackTrace(throwableProxy)
                : event.getFormattedMessage();
        SendEvent.sendException(message);

        if (ratelimitManager.checkAndSet(0L, MAX_ERRORS_PER_MINUTE, Duration.ofMinutes(1)).isPresent()) {
            return FilterReply.DENY;
        }

        if (message.contains("50007")) {
            return FilterReply.DENY;
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
