package commands;

public class CategoryCalculator {

    public static Category getCategoryByCommand(Class<? extends Command> c) {
        String packageName = c.getPackage().getName();
        if (packageName.endsWith("gimmickscategory")) return Category.GIMMICKS;
        if (packageName.endsWith("nsfwcategory")) return Category.NSFW;
        if (packageName.endsWith("configurationcategory")) return Category.CONFIGURATION;
        if (packageName.endsWith("utilitycategory")) return Category.UTILITY;
        if (packageName.endsWith("informationcategory")) return Category.INFORMATION;
        if (packageName.endsWith("splatoon2category")) return Category.SPLATOON_2;
        if (packageName.endsWith("interactionscategory")) return Category.INTERACTIONS;
        if (packageName.endsWith("externalcategory")) return Category.EXTERNAL;
        if (packageName.endsWith("fisherysettingscategory")) return Category.FISHERY_SETTINGS;
        if (packageName.endsWith("fisherycategory")) return Category.FISHERY;
        if (packageName.endsWith("casinocategory")) return Category.CASINO;
        if (packageName.endsWith("moderationcategory")) return Category.MODERATION;
        if (packageName.endsWith("aitoyscategory")) return Category.AI_TOYS;
        return null;
    }

}
