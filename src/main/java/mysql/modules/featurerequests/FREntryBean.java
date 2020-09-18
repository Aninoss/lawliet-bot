package mysql.modules.featurerequests;

public class FREntryBean {

    private final int id;
    private final boolean publicEntry;
    private final String description, title;
    private final int boosts;

    public FREntryBean(int id, boolean publicEntry, String title, String description, int boosts) {
        this.id = id;
        this.publicEntry = publicEntry;
        this.title = title;
        this.description = description;
        this.boosts = boosts;
    }

    public int getId() {
        return id;
    }

    public boolean isPublicEntry() {
        return publicEntry;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getBoosts() {
        return boosts;
    }

}
