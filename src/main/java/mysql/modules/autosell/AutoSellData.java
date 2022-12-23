package mysql.modules.autosell;

import java.util.Map;
import core.CustomObservableMap;
import core.cache.PatreonCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoSellData {

    private final CustomObservableMap<Long, AutoSellSlot> slotMap;

    public AutoSellData(@NonNull Map<Long, AutoSellSlot> slotMap) {
        this.slotMap = new CustomObservableMap<>(slotMap);
    }

    public CustomObservableMap<Long, AutoSellSlot> getSlotMap() {
        return slotMap;
    }

    public Integer getThreshold(long userId) {
        if (slotMap.containsKey(userId) && PatreonCache.getInstance().hasPremium(userId, false)) {
            return slotMap.get(userId).getThreshold();
        } else {
            return null;
        }
    }

    public void setThreshold(long userId, Integer threshold) {
        if (threshold != null) {
            slotMap.put(userId, new AutoSellSlot(userId, threshold));
        } else {
            slotMap.remove(userId);
        }
    }

}
