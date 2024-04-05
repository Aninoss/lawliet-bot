package core;

import java.time.Duration;
import java.time.Instant;

public class DiscordDomain {

    private static final String[] DOMAINS = System.getenv("DISCORD_DOMAINS").split(",");
    private static final String SECONDARY_DOMAIN = System.getenv("DISCORD_DOMAIN_SECONDARY");

    private static int domainIndex = 0;
    private static Instant useSecondaryDomainUntil = Instant.MIN;

    public static synchronized String get() {
        if (Instant.now().isBefore(useSecondaryDomainUntil) && SECONDARY_DOMAIN != null) {
            return SECONDARY_DOMAIN;
        }

        domainIndex = (domainIndex + 1) % DOMAINS.length;
        return DOMAINS[domainIndex];
    }

    public static void switchToSecondaryDomain() {
        useSecondaryDomainUntil = Instant.now().plus(Duration.ofHours(1));
        MainLogger.get().info("Switched to secondary domain until {}", useSecondaryDomainUntil);
    }

}
