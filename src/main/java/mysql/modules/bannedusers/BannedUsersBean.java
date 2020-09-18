package mysql.modules.bannedusers;

import core.CustomObservableList;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Observable;

public class BannedUsersBean extends Observable {

    private final CustomObservableList<Long> userIds;

    public BannedUsersBean(@NonNull ArrayList<Long> userIds) {
        this.userIds = new CustomObservableList<>(userIds);
    }


    /* Getters */

    public CustomObservableList<Long> getUserIds() {
        return userIds;
    }

}
