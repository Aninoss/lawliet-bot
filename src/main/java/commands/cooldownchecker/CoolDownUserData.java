package commands.cooldownchecker;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import constants.Settings;
import core.MainLogger;
import core.ShardManager;
import core.schedule.MainScheduler;
import net.dv8tion.jda.api.entities.User;

public class CoolDownUserData {

    private final long userId;
    private final ArrayList<Instant> commandInstants = new ArrayList<>();
    private boolean canPostCoolDownMessage = true;

    public CoolDownUserData(long userId) {
        this.userId = userId;
    }

    public Optional<Integer> getWaitingSec(int coolDown) {
        clean();

        if (commandInstants.size() >= Settings.COOLDOWN_MAX_ALLOWED) {
            MainLogger.get().warn("{} ({}) has hit a cool down", ShardManager.getInstance().getCachedUserById(userId).map(User::getAsTag).orElse("???"), userId);
            Duration duration = Duration.between(Instant.now(), commandInstants.get(0));
            MainScheduler.getInstance().schedule(commandInstants.get(0), "cool_down_post", () -> this.canPostCoolDownMessage = true);
            return Optional.of((int) (duration.getSeconds() + 1));
        }

        commandInstants.add(Instant.now().plusSeconds(coolDown));
        return Optional.empty();
    }

    public synchronized boolean canPostCoolDownMessage() {
        if (canPostCoolDownMessage) {
            canPostCoolDownMessage = false;
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