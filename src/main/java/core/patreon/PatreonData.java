package core.patreon;

import java.util.HashMap;
import java.util.HashSet;

public class PatreonData {

    private final HashMap<Long, Integer> userTierMap;
    private final HashSet<Long> guildList;
    private final HashSet<Long> guildPlusList;
    private final HashSet<Long> oldUserList;

    public PatreonData(HashMap<Long, Integer> userTierMap, HashSet<Long> guildList, HashSet<Long> guildPlusList, HashSet<Long> oldUserList) {
        this.userTierMap = userTierMap;
        this.guildList = guildList;
        this.guildPlusList = guildPlusList;
        this.oldUserList = oldUserList;
    }

    public HashMap<Long, Integer> getUserTierMap() {
        return userTierMap;
    }

    public HashSet<Long> getGuildList() {
        return guildList;
    }

    public HashSet<Long> getGuildPlusList() {
        return guildPlusList;
    }

    public HashSet<Long> getOldUserList() {
        return oldUserList;
    }

}
