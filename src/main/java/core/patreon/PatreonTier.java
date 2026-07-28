package core.patreon;

public enum PatreonTier {

    BASIC("Basic", 703303395867492453L, false),
    PRO_1("Pro (1 Server)", 704721905453629481L, true),
    PRO_2("Pro (2 Servers)", 704721939968688249L, true),
    PRO_3("Pro (3 Servers)", 706143381784494132L, true),
    PRO_PLUS_1("Pro+ (1 Server)", 706143478085582898L, true),
    PRO_PLUS_2("Pro+ (2 Servers)", 762322081840234506L, true),
    PRO_PLUS_3("Pro+ (3 Servers)", 1530927925216411698L, true);


    private final String name;
    private final long roleId;
    private final boolean unlocksServers;

    PatreonTier(String name, long roleId, boolean unlocksServers) {
        this.name = name;
        this.roleId = roleId;
        this.unlocksServers = unlocksServers;
    }

    public String getName() {
        return name;
    }

    public long getRoleId() {
        return roleId;
    }

    public boolean getUnlocksServers() {
        return unlocksServers;
    }
}
