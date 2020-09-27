package commands.cooldownchecker;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

public class CooldownData {

    private Thread thread = null;
    private final ArrayList<Instant> commandInstants = new ArrayList<>();

    public Optional<Integer> getWaitingSec(int cooldown) {
        clean();

        if (commandInstants.size() >= CooldownManager.MAX_ALLOWED) {
            Duration duration = Duration.between(Instant.now(), commandInstants.get(0));
            return Optional.of((int) (duration.getSeconds() + 1));
        }

        commandInstants.add(Instant.now().plusSeconds(cooldown));
        return Optional.empty();
    }

    public boolean isPostingFree() {
        if (thread == null || !thread.isAlive()) {
            thread = Thread.currentThread();
            return true;
        }
        return false;
    }

    private void clean() {
        while(commandInstants.size() > 0 && commandInstants.get(0).isBefore(Instant.now()))
            commandInstants.remove(0);
    }

    public boolean isEmpty() {
        clean();
        return commandInstants.isEmpty();
    }

}