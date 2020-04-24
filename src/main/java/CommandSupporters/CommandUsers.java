package CommandSupporters;

import java.util.ArrayList;
import java.util.HashMap;

public class CommandUsers {

    private static final CommandUsers ourInstance = new CommandUsers();
    private CommandUsers() {}
    public static CommandUsers getInstance() { return ourInstance; }

    private ArrayList<Long> users = new ArrayList<>();

    public void addUsage(long userId) {
        if (!users.contains(userId)) users.add(userId);
    }

    public int checkDailyUniqueUsers() {
        int size = users.size();
        users = new ArrayList<>();
        return size;
    }

}
