package commands.cooldownchecker;

import java.util.HashMap;
import java.util.Optional;

public class CooldownManager {

    private static final CooldownManager ourInstance = new CooldownManager();
    public static CooldownManager getInstance() {
        return ourInstance;
    }
    private CooldownManager() {}

    public static final int MAX_ALLOWED = 2;

    private final HashMap<Long, CooldownData> cooldownDataMap = new HashMap<>();

    public Optional<Integer> getWaitingSec(long userId, int cooldown) {
        return cooldownDataMap.computeIfAbsent(userId, uid -> new CooldownData()).getWaitingSec(cooldown);
    }

    public boolean isFree(long userId) {
        CooldownData data = cooldownDataMap.get(userId);
        if (data != null) return data.isPostingFree();
        return true;
    }

    public void clean() {
        cooldownDataMap.entrySet().removeIf(set -> set == null || !set.getValue().isEmpty());
    }

}