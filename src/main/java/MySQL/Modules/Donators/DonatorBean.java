package MySQL.Modules.Donators;

import Core.CustomObservableMap;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Observable;

public class DonatorBean extends Observable {

    private final CustomObservableMap<Long, DonatorBeanSlot> slots;

    public DonatorBean(HashMap<Long, DonatorBeanSlot> slots) { this.slots = new CustomObservableMap<>(slots); }


    /* Getters */

    public CustomObservableMap<Long, DonatorBeanSlot> getMap() { return slots; }

    public DonatorBeanSlot get(long userId) {
        return slots.computeIfAbsent(userId, key -> new DonatorBeanSlot(
                userId,
                LocalDate.now(),
                0
        ));
    }

}
