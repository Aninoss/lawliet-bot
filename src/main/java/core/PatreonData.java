package core;

import java.util.HashSet;

public class PatreonData {

    private final HashSet<Long> userList;
    private final HashSet<Long> guildList;
    private final HashSet<Long> oldUserList;
    private final HashSet<Long> highPayingUserList;

    public PatreonData(HashSet<Long> userList, HashSet<Long> guildList, HashSet<Long> oldUserList, HashSet<Long> highPayingUserList) {
        this.userList = userList;
        this.guildList = guildList;
        this.oldUserList = oldUserList;
        this.highPayingUserList = highPayingUserList;
    }

    public HashSet<Long> getUserList() {
        return userList;
    }

    public HashSet<Long> getGuildList() {
        return guildList;
    }

    public HashSet<Long> getOldUserList() {
        return oldUserList;
    }

    public HashSet<Long> getHighPayingUserList() {
        return highPayingUserList;
    }

}
