package General.Survey;

import java.time.Instant;

public class Survey {

    private int id;
    private Instant start;

    public Survey(int id, Instant start) {
        this.id = id;
        this.start = start;
    }

    public int getId() {
        return id;
    }

    public Instant getStart() {
        return start;
    }
}
