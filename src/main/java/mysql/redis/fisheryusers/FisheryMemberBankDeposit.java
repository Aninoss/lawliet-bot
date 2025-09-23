package mysql.redis.fisheryusers;

import java.time.Instant;

public class FisheryMemberBankDeposit {

    private final long coins;
    private final Instant until;

    public FisheryMemberBankDeposit(long coins, Instant until) {
        this.coins = coins;
        this.until = until;
    }

    public FisheryMemberBankDeposit(String input) {
        String[] values = input.split(":");
        this.coins = Long.parseLong(values[0]);
        this.until = Instant.parse(values[1]);
    }

    public long getCoins() {
        return coins;
    }

    public Instant getUntil() {
        return until;
    }

    @Override
    public String toString() {
        return coins + ":" + until.toString();
    }

}
