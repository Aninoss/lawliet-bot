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

    public boolean isActive(long userId) {
        return userList.contains(userId) && PatreonCache.getInstance().getUserTier(userId) >= 2;
    }

    public void setActive(long userId, boolean active) {
        if (active && !userList.contains(userId))
            userList.add(userId);
        else if (!active)
            userList.remove(userId);
    }

}
