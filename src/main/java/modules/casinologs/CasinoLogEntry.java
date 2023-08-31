package modules.casinologs;

import java.time.Instant;
import java.util.ArrayList;

public class CasinoLogEntry {

    private final String command;
    private Instant lastEventTime = Instant.now();
    private final ArrayList<String> events = new ArrayList<>();

    public CasinoLogEntry(String command) {
        this.command = command;
    }

    public void addEvent(String event) {
        events.add(event);
        lastEventTime = Instant.now();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Command: " + command + "; Last Event: " + lastEventTime);
        for (String event : events) {
            sb.append("\n\t- ").append(event);
        }
        return sb.toString();
    }
}
