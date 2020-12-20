package mysql.modules.patreon;

import java.time.LocalDate;

public class PatreonBean {

    private final long userId;
    private final int tier;
    private final LocalDate expirationDate;

    public PatreonBean(long userId, int tier, LocalDate expirationDate) {
        this.userId = userId;
        this.tier = tier;
        this.expirationDate = expirationDate;
    }

    /* Getters */

    public long getUserId() {
        return userId;
    }

    public int getTier() {
        return tier;
    }

    public boolean isValid() {
        return expirationDate.isAfter(LocalDate.now());
    }

}
