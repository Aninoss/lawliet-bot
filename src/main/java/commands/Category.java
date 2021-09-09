package commands;

import java.util.Arrays;

public enum Category {

    GIMMICKS("gimmicks", true),
    AI_TOYS("aitoys", true),
    CONFIGURATION("configuration", true),
    UTILITY("utility", true),
    MODERATION("moderation", true),
    INFORMATION("information", true),
    FISHERY_SETTINGS("fishery_settings_category", true),
    FISHERY("fishery_category", true),
    CASINO("casino", true),
    INTERACTIONS("interactions", true),
    EXTERNAL("external_services", true),
    NSFW("nsfw", true),
    SPLATOON_2("splatoon_2", true),
    PATREON_ONLY("patreon_only", false);

    private final String id;
    private final boolean independent;

    Category(String id, boolean independent) {
        this.id = id;
        this.independent = independent;
    }

    public String getId() {
        return id;
    }

    public boolean isIndependent() {
        return independent;
    }

    public static Category[] independentValues() {
        return Arrays.stream(values())
                .filter(Category::isIndependent)
                .toArray(Category[]::new);
    }

}