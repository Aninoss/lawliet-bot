package mysql.modules.autowork;

import java.util.List;
import core.CustomObservableList;
import core.cache.PatreonCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoWorkData {

    private final CustomObservableList<Long> userList;

    public AutoWorkData(@NonNull List<Long> userList) {
        this.userList = new CustomObservableList<>(userList);
    }

    public CustomObservableList<Long> getUserList() {
        return userList;
    }

    public boolean isActive(long userId) {
        return userList.contains(userId) && PatreonCache.getInstance().hasPremium(userId, false);
    }

    public void setActive(long userId, boolean active) {
        if (active && !userList.contains(userId)) {
            userList.add(userId);
        } else if (!active) {
            userList.remove(userId);
        }
    }

}
