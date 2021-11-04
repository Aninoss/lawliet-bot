package core;

import java.util.HashMap;
import java.util.HashSet;

public class PatreonData {

    private final HashMap<Long, Integer> userTierMap;
    private final HashSet<Long> guildList;
    private final HashSet<Long> oldUserList;

    public PatreonData(HashMap<Long, Integer> userTierMap, HashSet<Long> guildList, HashSet<Long> oldUserList) {
        this.userTierMap = userTierMap;
        this.guildList = guildList;
        this.oldUserList = oldUserList;
    }

    public HashMap<Long, Integer> getUserTierMap() {
        return userTierMap;
    }

    public HashSet<Long> getGuildList() {
        return guildList;
    }

    public HashSet<Long> getOldUserList() {
        return oldUserList;
    }

}
