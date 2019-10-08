package CommandSupporters;

import Constants.Category;
import General.Shortcuts;
import org.javacord.api.DiscordApi;

public class CategoryCalculator {

    public static String getCategoryByCommand(Class c) {
        String packageName = c.getPackage().getName();
        if (packageName.equals("Commands.Gimmicks")) return Category.GIMMICKS;
        if (packageName.equals("Commands.BotOwner")) return Category.BOT_OWNER;
        if (packageName.equals("Commands.NSFW")) return Category.NSFW;
        if (packageName.equals("Commands.Management")) return Category.MANAGEMENT;
        if (packageName.equals("Commands.Information")) return Category.INFORMATION;
        if (packageName.equals("Commands.Splatoon2")) return Category.SPLATOON_2;
        if (packageName.equals("Commands.Emotes")) return Category.EMOTES;
        if (packageName.equals("Commands.Interactions")) return Category.INTERACTIONS;
        if (packageName.equals("Commands.External")) return Category.EXTERNAL;
        if (packageName.equals("Commands.FisheryCategory")) return Category.FISHERY;
        if (packageName.equals("Commands.Casino")) return Category.CASINO;
        if (packageName.equals("Commands.Moderation")) return Category.MODERATION;
        return null;
    }

}
