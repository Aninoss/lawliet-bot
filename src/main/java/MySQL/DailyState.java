package MySQL;

public class DailyState {
    private int streak;
    private boolean claimed, streakBroken;

    public DailyState(int streak, boolean claimed, boolean streakBroken) {
        this.streak = streak;
        this.claimed = claimed;
        this.streakBroken = streakBroken;
    }

    public int getStreak() {
        return streak;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public boolean isStreakBroken() {
        return streakBroken;
    }
}
