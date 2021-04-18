package mysql.modules.bannedusers;

import java.util.ArrayList;
import java.util.Observable;
import core.CustomObservableList;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BannedUsersData extends Observable {

    private final CustomObservableList<Long> userIds;

    public BannedUsersData(@NonNull ArrayList<Long> userIds) {
        this.userIds = new CustomObservableList<>(userIds);
    }

    public CustomObservableList<Long> getUserIds() {
        return userIds;
    }

}
