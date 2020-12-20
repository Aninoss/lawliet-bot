package mysql.modules.autoclaim;

import core.CustomObservableList;
import core.patreon.PatreonApi;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.util.ArrayList;

public class AutoClaimBean {

    CustomObservableList<Long> userList;

    public AutoClaimBean(@NonNull ArrayList<Long> userList) {
        this.userList = new CustomObservableList<>(userList);
    }


    /* Getters */

    public CustomObservableList<Long> getUserList() {
        return userList;
    }

    public boolean isActive(long userId) {
        return userList.contains(userId) && PatreonApi.getInstance().getUserTier(userId) >= 2;
    }

    public void setActive(long userId, boolean active) {
        if (active && !userList.contains(userId))
            userList.add(userId);
        else if (!active)
            userList.remove(userId);
    }

}
