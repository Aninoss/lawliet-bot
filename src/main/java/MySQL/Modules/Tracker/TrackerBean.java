package MySQL.Modules.Tracker;

import Core.Bot;
import Core.CustomObservableMap;
import ServerStuff.Discordbotlist;
import javafx.util.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Observable;

public class TrackerBean extends Observable {

    final static Logger LOGGER = LoggerFactory.getLogger(TrackerBean.class);

    private final CustomObservableMap<Pair<Long, String>, TrackerBeanSlot> slots;

    public TrackerBean(@NonNull HashMap<Pair<Long, String>, TrackerBeanSlot> slots) {
        this.slots = new CustomObservableMap<>(slots);
        this.slots.values().forEach(slot -> {
            try {
                if (slot.getServer().isPresent()) {
                    if (Bot.isProductionMode()) Thread.sleep(500);
                    slot.start();
                }
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        });
    }


    /* Getters */

    public CustomObservableMap<Pair<Long, String>, TrackerBeanSlot> getMap() { return slots; }

}
