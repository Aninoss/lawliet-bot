package mysql.modules.casinostats;

public class CasinoStatsSlot {

    private final String id;
    private final String game;
    private final boolean won;
    private final long value;

    public CasinoStatsSlot(String id, String game, boolean won, long value) {
        this.id = id;
        this.game = game;
        this.won = won;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getGame() {
        return game;
    }

    public boolean isWon() {
        return won;
    }

    public long getValue() {
        return value;
    }

}
