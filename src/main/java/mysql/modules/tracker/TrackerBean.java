package mysql.modules.tracker;

import java.util.ArrayList;
import java.util.Observable;
import core.CustomObservableList;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TrackerBean extends Observable {

    private final CustomObservableList<TrackerBeanSlot> slots;

    public TrackerBean(@NonNull ArrayList<TrackerBeanSlot> slots) {
        this.slots = new CustomObservableList<>(slots);
    }



    /* Getters */

    public synchronized CustomObservableList<TrackerBeanSlot> getSlots() {
        return slots;
    }

}
