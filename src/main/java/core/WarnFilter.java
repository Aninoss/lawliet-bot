package core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.time.Duration;
import java.util.Objects;

public class WarnFilter extends Filter<ILoggingEvent> {

    public static int MAX_429_PER_MINUTE = Integer.parseInt(Objects.requireNonNullElse(System.getenv("MAX_429_PER_MINUTE"), "1000"));

    private final RatelimitManager errorResponseEmergencyStopper = new RatelimitManager();

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        if (event.getFormattedMessage().contains("Encountered 429") &&
                errorResponseEmergencyStopper.checkAndSet(0L, MAX_429_PER_MINUTE, Duration.ofMinutes(1)).isPresent()
        ) {
            MainLogger.get().error("EXIT - 429 error codes exceeding threshold");
            System.exit(11);
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }

}
