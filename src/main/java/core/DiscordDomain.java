package core;

public class DiscordDomain {

    private static String domain = System.getenv("DISCORD_DOMAIN");

    public static synchronized String get() {
        return domain;
    }

    public static void set(String newDomain) {
        domain = newDomain;
    }

}
