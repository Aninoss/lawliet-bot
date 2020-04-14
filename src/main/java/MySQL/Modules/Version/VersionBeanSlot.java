package MySQL.Modules.Version;

import java.time.Instant;

public class VersionBeanSlot {

    private final String version;
    private final Instant date;

    public VersionBeanSlot(String version, Instant date) {
        this.version = version;
        this.date = date;
    }

    public String getVersion() { return version; }

    public Instant getDate() { return date; }

}
