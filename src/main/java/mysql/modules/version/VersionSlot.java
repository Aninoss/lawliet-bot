package mysql.modules.version;

import java.time.Instant;

public class VersionSlot {

    private final String version;
    private final Instant date;

    public VersionSlot(String version, Instant date) {
        this.version = version;
        this.date = date;
    }

    public String getVersion() {
        return version;
    }

    public Instant getDate() {
        return date;
    }

}
