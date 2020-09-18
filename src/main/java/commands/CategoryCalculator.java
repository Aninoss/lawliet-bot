package commands;

import constants.Category;

public class CategoryCalculator {

    public static String getCategoryByCommand(Class<? extends Command> c) {
        String packageName = c.getPackage().getName();
        if (packageName.endsWith("gimmickscategory")) return Category.GIMMICKS;
        if (packageName.endsWith("nsfwcategory")) return Category.NSFW;
        if (packageName.endsWith("managementcategory")) return Category.MANAGEMENT;
        if (packageName.endsWith("informationcategory")) return Category.INFORMATION;
        if (packageName.endsWith("splatoon2category")) return Category.SPLATOON_2;
        if (packageName.endsWith("emotescategory")) return Category.EMOTES;
        if (packageName.endsWith("interactionscategory")) return Category.INTERACTIONS;
        if (packageName.endsWith("externalcategory")) return Category.EXTERNAL;
        if (packageName.endsWith("fisherysettingscategory")) return Category.FISHERY_SETTINGS;
        if (packageName.endsWith("fisherycategory")) return Category.FISHERY;
        if (packageName.endsWith("casinocategory")) return Category.CASINO;
        if (packageName.endsWith("moderationcategory")) return Category.MODERATION;
        return null;
    }

}
