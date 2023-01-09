package mysql.modules.casinotracking;

import java.util.List;
import core.CustomObservableList;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CasinoTrackingData {

    private final CustomObservableList<Long> userList;

    public CasinoTrackingData(@NonNull List<Long> userList) {
        this.userList = new CustomObservableList<>(userList);
    }

    public CustomObservableList<Long> getUserList() {
        return userList;
    }

    public boolean isActive(long userId) {
        return userList.contains(userId);
    }

    public void setActive(long userId, boolean active) {
        if (active && !userList.contains(userId)) {
            userList.add(userId);
        } else if (!active) {
            userList.remove(userId);
        }
    }

}
