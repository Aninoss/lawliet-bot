package General.Warnings;

import java.time.Instant;
import java.util.ArrayList;

public class UserWarnings {

    private ArrayList<WarningSlot> slots = new ArrayList<>();

    public void add(WarningSlot warningSlot) {
        slots.add(warningSlot);
    }

    public ArrayList<WarningSlot> getLatest(int n) {
        ArrayList<WarningSlot> newSlots = new ArrayList<>();
        for(int i = 0; i < Math.min(n, slots.size()); i++) newSlots.add(slots.get(i));
        return newSlots;
    }

    public int amountLatestDays(int days) {
        return amountLatestHours(days * 24);
    }

    public int amountLatestHours(int hours) {
        return amountLatestSeconds(hours * 60 * 60);
    }

    public int amountLatestSeconds(int seconds) {
        return (int) slots.stream()
                .filter(slot -> slot.getTime().isAfter(Instant.now().minusSeconds(seconds)))
                .count();
    }

    public int amountTotal() {
        return slots.size();
    }

}
