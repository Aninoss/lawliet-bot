package modules;

import constants.AssetIds;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AninossRaidProtection {

    private static final AninossRaidProtection ourInstance = new AninossRaidProtection();

    public static AninossRaidProtection getInstance() {
        return ourInstance;
    }

    private AninossRaidProtection() {
    }

    private User lastUser = null;
    private Instant lastInstant = null;

    public synchronized boolean check(User user, Role role) {
        if (role.getServer().getId() != AssetIds.ANICORD_SERVER_ID)
            return true;

        boolean ok = lastUser == null || lastInstant == null || lastInstant.plus(1, ChronoUnit.MINUTES).isBefore(Instant.now());
        if (!ok) {
            role.getServer().getMemberById(lastUser.getId()).ifPresent(u -> {
                role.removeUser(u).exceptionally(ExceptionLogger.get());
            });
        }

        lastUser = user;
        lastInstant = Instant.now();
        return ok;
    }

}
