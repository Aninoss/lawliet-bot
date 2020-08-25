package MySQL.Modules.FeatureRequests;

public class FREntry {

    private final int id;
    private final boolean publicEntry;
    private final String description;
    private final int boosts;

    public FREntry(int id, boolean publicEntry, String description, int boosts) {
        this.id = id;
        this.publicEntry = publicEntry;
        this.description = description;
        this.boosts = boosts;
    }

    public int getId() {
        return id;
    }

    public boolean isPublicEntry() {
        return publicEntry;
    }

    public String getDescription() {
        return description;
    }

    public int getBoosts() {
        return boosts;
    }

}
