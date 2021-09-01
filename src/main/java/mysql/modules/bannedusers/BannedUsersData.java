package mysql.modules.bannedusers;

import java.util.List;
import java.util.Observable;
import core.CustomObservableList;

public class BannedUsersData extends Observable {

    private final CustomObservableList<Long> userIds;

    public BannedUsersData(List<Long> userIds) {
        this.userIds = new CustomObservableList<>(userIds);
    }

    public CustomObservableList<Long> getUserIds() {
        return userIds;
    }

}
