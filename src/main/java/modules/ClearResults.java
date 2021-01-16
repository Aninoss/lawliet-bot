package modules;

public class ClearResults {

    final int deleted;
    final int remaining;

    public ClearResults(int deleted, int remaining) {
        this.deleted = deleted;
        this.remaining = remaining;
    }

    public int getDeleted() {
        return deleted;
    }

    public int getRemaining() {
        return remaining;
    }

}
