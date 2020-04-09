package MySQL.Modules.Tracker;

import Core.CustomObservableMap;
import javafx.util.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Observable;

public class TrackerBean extends Observable {

    private CustomObservableMap<Pair<Long, String>, TrackerBeanSlot> slots;

    public TrackerBean(@NonNull HashMap<Pair<Long, String>, TrackerBeanSlot> slots) { this.slots = new CustomObservableMap<>(slots); }


    /* Getters */

    public CustomObservableMap<Pair<Long, String>, TrackerBeanSlot> getMap() { return slots; }

}
