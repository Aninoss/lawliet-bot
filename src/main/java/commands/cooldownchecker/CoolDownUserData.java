package commands.cooldownchecker;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import constants.Settings;
import core.schedule.MainScheduler;

public class CoolDownUserData {

    private final ArrayList<Instant> commandInstants = new ArrayList<>();
    private boolean canPostCoolDownMessage = true;

    public Optional<Integer> getWaitingSec(int coolDown) {
        clean();

        if (commandInstants.size() >= Settings.COOLDOWN_MAX_ALLOWED) {
            Duration duration = Duration.between(Instant.now(), commandInstants.get(0));
            return Optional.of((int) (duration.getSeconds() + 1));
        }

        commandInstants.add(Instant.now().plusSeconds(coolDown));
        return Optional.empty();
    }

    public synchronized boolean canPostCoolDownMessage() {
        if (canPostCoolDownMessage) {
            canPostCoolDownMessage = false;
            MainScheduler.getInstance().schedule(15, ChronoUnit.SECONDS, "cool_down", () -> this.canPostCoolDownMessage = true);
            return true;
        }

        return false;
    }

    private void clean() {
        while (commandInstants.size() > 0 && commandInstants.get(0).isBefore(Instant.now())) {
            commandInstants.remove(0);
        }
    }

    public boolean isEmpty() {
        clean();
        return commandInstants.isEmpty();
    }

}