package CommandSupporters.Cooldown;

import java.util.HashMap;
import java.util.Optional;

public class Cooldown {

    private static Cooldown ourInstance = new Cooldown();
    public static Cooldown getInstance() {
        return ourInstance;
    }
    private Cooldown() {}

    public static final int COOLDOWN_TIME_IN_SECONDS = 10;
    public static final int MAX_ALLOWED = 2;

    private HashMap<Long, CooldownData> cooldownDataMap = new HashMap<>();

    public Optional<Integer> getWaitingSec(long userId) {
        return cooldownDataMap.computeIfAbsent(userId, uid -> new CooldownData()).getWaitingSec();
    }

    public boolean isFree(long userId) {
        CooldownData data = cooldownDataMap.get(userId);
        if (data != null) return data.isPostingFree();
        return true;
    }

    public synchronized void clean() {
        cooldownDataMap.entrySet().removeIf(set -> !set.getValue().isEmpty());
    }

}
