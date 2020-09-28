package mysql.modules.tracker;

import core.CustomObservableList;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Observable;

public class TrackerBean extends Observable {

    private final CustomObservableList<TrackerBeanSlot> slots;

    public TrackerBean(@NonNull ArrayList<TrackerBeanSlot> slots) {
        this.slots = new CustomObservableList<>(slots);
    }



    /* Getters */

    public CustomObservableList<TrackerBeanSlot> getSlots() {
        return slots;
    }

}
