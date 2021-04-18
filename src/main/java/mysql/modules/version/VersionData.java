package mysql.modules.version;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.stream.Collectors;
import core.CustomObservableList;

public class VersionData extends Observable {

    private final CustomObservableList<VersionSlot> slots;

    public VersionData(ArrayList<VersionSlot> slots) {
        this.slots = new CustomObservableList<>(slots);
    }

    public CustomObservableList<VersionSlot> getSlots() {
        return slots;
    }

    public VersionSlot getCurrentVersion() {
        return getCurrentVersions(1).get(0);
    }

    public List<VersionSlot> getCurrentVersions(int n) {
        return slots.stream()
                .skip(Math.max(0, slots.size() - Math.min(5, n)))
                .collect(Collectors.toList());
    }

}
