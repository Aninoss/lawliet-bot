package core;

import java.util.HashMap;
import java.util.List;

public class PatreonData {

    private final HashMap<Long, Integer> userMap;
    private final List<Long> guildList;

    public PatreonData(HashMap<Long, Integer> userMap, List<Long> guildList) {
        this.userMap = userMap;
        this.guildList = guildList;
    }

    public HashMap<Long, Integer> getUserMap() {
        return userMap;
    }

    public List<Long> getGuildList() {
        return guildList;
    }

}
