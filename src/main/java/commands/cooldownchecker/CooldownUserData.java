package commands.cooldownchecker;

import constants.Settings;
import core.schedule.MainScheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

public class CooldownUserData {

    private final ArrayList<Instant> commandInstants = new ArrayList<>();
    private boolean canPostCooldownMessage = true;

    public Optional<Integer> getWaitingSec(int cooldown) {
        clean();

        if (commandInstants.size() >= Settings.COOLDOWN_MAX_ALLOWED) {
            Duration duration = Duration.between(Instant.now(), commandInstants.get(0));
            return Optional.of((int) (duration.getSeconds() + 1));
        }

        commandInstants.add(Instant.now().plusSeconds(cooldown));
        return Optional.empty();
    }

    public synchronized boolean canPostCooldownMessage() {
        if (canPostCooldownMessage) {
            canPostCooldownMessage = false;
            MainScheduler.getInstance().schedule(5, ChronoUnit.SECONDS, "cooldown", () -> this.canPostCooldownMessage = true);
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