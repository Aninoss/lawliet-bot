package commands;

import java.time.Instant;
import java.util.function.Function;

public class CommandListenerMeta <T> {

    private final long authorId;
    private final Function<T, Boolean> validityChecker;
    private final Command command;
    private final Runnable onTimeOut;
    private final Runnable onOverridden;
    private final Instant creationTime = Instant.now();

    public CommandListenerMeta(long authorId, Function<T, Boolean> validityChecker, Runnable onTimeOut, Runnable onOverridden, Command command) {
        this.authorId = authorId;
        this.validityChecker = validityChecker;
        this.onTimeOut = onTimeOut;
        this.onOverridden = onOverridden;
        this.command = command;
    }

    public long getAuthorId() {
        return authorId;
    }

    public boolean check(Object o) {
        return validityChecker.apply((T)o);
    }

    public void timeOut() {
        onTimeOut.run();
    }

    public void override() {
        onOverridden.run();
    }

    public Command getCommand() {
        return command;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

}
