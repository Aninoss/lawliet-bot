package mysql.modules.autoclaim;

import java.util.List;
import core.CustomObservableList;
import core.cache.PatreonCache;

public class AutoClaimData {

    private final CustomObservableList<Long> userList;

    public AutoClaimData(List<Long> userList) {
        this.userList = new CustomObservableList<>(userList);
    }

    public CustomObservableList<Long> getUserList() {
        return userList;
    }

    public boolean isActive(long userId) {
        return userList.contains(userId) && PatreonCache.getInstance().getUserTier(userId, false) >= 2;
    }

    public void setActive(long userId, boolean active) {
        if (active && !userList.contains(userId)) {
            userList.add(userId);
        } else if (!active) {
            userList.remove(userId);
        }
    }

}
