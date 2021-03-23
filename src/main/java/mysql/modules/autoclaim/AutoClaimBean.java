package mysql.modules.autoclaim;

import java.util.ArrayList;
import core.CustomObservableList;
import core.cache.PatreonCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoClaimBean {

    private final CustomObservableList<Long> userList;

    public AutoClaimBean(@NonNull ArrayList<Long> userList) {
        this.userList = new CustomObservableList<>(userList);
    }


    /* Getters */

    public CustomObservableList<Long> getUserList() {
        return userList;
    }

    public boolean isActive(long guildId, long userId) {
        boolean patreon = PatreonCache.getInstance().getUserTier(userId, true) >= 2 || PatreonCache.getInstance().isUnlocked(guildId);
        return userList.contains(userId) && patreon;
    }

    public void setActive(long userId, boolean active) {
        if (active && !userList.contains(userId)) {
            userList.add(userId);
        } else if (!active) {
            userList.remove(userId);
        }
    }

}
