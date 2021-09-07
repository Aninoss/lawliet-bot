package commands;

public interface Category {

    String GIMMICKS = "gimmicks",
            UTILITY = "utility",
            AI_TOYS = "aitoys",
            CONFIGURATION = "configuration",
            INFORMATION = "information",
            INTERACTIONS = "interactions",
            EXTERNAL = "external_services",
            SPLATOON_2 = "splatoon_2",
            NSFW = "nsfw",
            FISHERY = "fishery_category",
            FISHERY_SETTINGS = "fishery_settings_category",
            CASINO = "casino",
            MODERATION = "moderation",
            PATREON_ONLY = "patreon_only";

    String[] LIST = new String[] {
            GIMMICKS,
            AI_TOYS,
            CONFIGURATION,
            UTILITY,
            MODERATION,
            INFORMATION,
            FISHERY_SETTINGS,
            FISHERY,
            CASINO,
            INTERACTIONS,
            EXTERNAL,
            NSFW,
            SPLATOON_2
    };

}