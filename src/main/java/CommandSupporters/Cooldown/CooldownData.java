package CommandSupporters.Cooldown;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

public class CooldownData {

    private Thread thread = null;
    private ArrayList<Instant> commandInstants = new ArrayList<>();

    public Optional<Integer> getWaitingSec() {
        clean();

        if (commandInstants.size() >= Cooldown.MAX_ALLOWED) {
            Duration duration = Duration.between(Instant.now().minusSeconds(Cooldown.COOLDOWN_TIME_IN_SECONDS), commandInstants.get(0));
            return Optional.of((int) (duration.getSeconds() + 1));
        }

        commandInstants.add(Instant.now());
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
        while(commandInstants.size() > 0 && commandInstants.get(0).isBefore(Instant.now().minusSeconds(Cooldown.COOLDOWN_TIME_IN_SECONDS)))
            commandInstants.remove(0);
    }

    public boolean isEmpty() {
        clean();
        return commandInstants.isEmpty();
    }

}