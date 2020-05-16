package CommandSupporters;

import Constants.Category;

public class CategoryCalculator {

    public static String getCategoryByCommand(Class<? extends Command> c) {
        String packageName = c.getPackage().getName();
        if (packageName.equals("Commands.GimmicksCategory")) return Category.GIMMICKS;
        if (packageName.equals("Commands.NSFWCategory")) return Category.NSFW;
        if (packageName.equals("Commands.ManagementCategory")) return Category.MANAGEMENT;
        if (packageName.equals("Commands.InformationCategory")) return Category.INFORMATION;
        if (packageName.equals("Commands.Splatoon2Category")) return Category.SPLATOON_2;
        if (packageName.equals("Commands.EmotesCategory")) return Category.EMOTES;
        if (packageName.equals("Commands.InteractionsCategory")) return Category.INTERACTIONS;
        if (packageName.equals("Commands.ExternalCategory")) return Category.EXTERNAL;
        if (packageName.equals("Commands.FisheryCategory")) return Category.FISHERY;
        if (packageName.equals("Commands.CasinoCategory")) return Category.CASINO;
        if (packageName.equals("Commands.ModerationCategory")) return Category.MODERATION;
        return null;
    }

}
