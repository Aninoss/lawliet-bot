package mysql.modules.bannedusers;

import java.util.Map;
import core.CustomObservableMap;

public class BannedUsersData {

    private final CustomObservableMap<Long, BannedUserSlot> slotsMap;

    public BannedUsersData(Map<Long, BannedUserSlot> slotsMap) {
        this.slotsMap = new CustomObservableMap<>(slotsMap);
    }

    public CustomObservableMap<Long, BannedUserSlot> getSlotsMap() {
        return slotsMap;
    }

}
