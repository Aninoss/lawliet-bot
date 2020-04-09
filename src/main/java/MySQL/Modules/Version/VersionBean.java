package MySQL.Modules.Version;

import Core.CustomObservableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.stream.Collectors;

public class VersionBean extends Observable {

    private CustomObservableList<VersionBeanSlot> slots;

    public VersionBean(ArrayList<VersionBeanSlot> slots) {
        this.slots = new CustomObservableList<>(slots);
    }

    public CustomObservableList<VersionBeanSlot> getSlots() { return slots; }

    public VersionBeanSlot getCurrentVersion() {
        return getCurrentVersions(1).get(0);
    }

    public List<VersionBeanSlot> getCurrentVersions(int n) {
        return slots.stream()
                .skip(Math.max(0, slots.size() - Math.min(5, n)))
                .collect(Collectors.toList());
    }

}
