package General.Warnings;

import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.Optional;

public class WarningSlot {

    private Instant time;
    private String reason;
    private User requestor;

    public WarningSlot(Instant time, User requestor, String reason) {
        this.time = time;
        this.requestor = requestor;
        this.reason = reason;
    }

    public WarningSlot(Instant time) {
        this.time = time;
    }

    public Instant getTime() {
        return time;
    }

    public Optional<User> getRequestor() {
        return Optional.of(requestor);
    }

    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }
}
