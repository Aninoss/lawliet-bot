package mysql.modules.tracker;

import core.CustomObservableList;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Observable;

public class TrackerBean extends Observable {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerBean.class);

    private final CustomObservableList<TrackerBeanSlot> slots;

    public TrackerBean(@NonNull ArrayList<TrackerBeanSlot> slots) {
        this.slots = new CustomObservableList<>(slots);
    }



    /* Getters */

    public synchronized CustomObservableList<TrackerBeanSlot> getSlots() {
        slots.removeIf(o -> {
            if (Objects.isNull(o)) {
                LOGGER.warn("NULL VALUE IN ALERTS"); //TODO Debug
                return true;
            }
            return false;
        });
        return slots;
    }

}
