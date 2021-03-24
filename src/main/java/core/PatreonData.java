package core;

import java.util.HashMap;
import java.util.List;

public class PatreonData {

    private final HashMap<Long, Integer> userMap;
    private final List<Long> guildList;
    private final List<Long> oldUsersList;

    public PatreonData(HashMap<Long, Integer> userMap, List<Long> guildList, List<Long> oldUsersList) {
        this.userMap = userMap;
        this.guildList = guildList;
        this.oldUsersList = oldUsersList;
    }

    public HashMap<Long, Integer> getUserMap() {
        return userMap;
    }

    public List<Long> getGuildList() {
        return guildList;
    }

    public List<Long> getOldUsersList() {
        return oldUsersList;
    }

}
