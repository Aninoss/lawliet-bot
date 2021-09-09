package modules;

public enum OsuGame {

    OSU("osu"),
    TAIKO("taiko"),
    CATCH("fruits"),
    MANIA("mania");

    private final String id;

    OsuGame(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
