package CommandSupporters;

import Constants.Category;
import General.Shortcuts;
import org.javacord.api.DiscordApi;

public class CategoryCalculator {
    public static String getCategoryByCommand(Class c) {
        String packageName = c.getPackage().getName();
        if (packageName.equals("Commands.General")) return Category.GENERAL;
        if (packageName.equals("Commands.BotOwner")) return Category.BOT_OWNER;
        if (packageName.equals("Commands.NSFW")) return Category.NSFW;
        if (packageName.equals("Commands.ServerManagement")) return Category.SERVER_MANAGEMENT;
        if (packageName.equals("Commands.BotManagement")) return Category.BOT_MANAGEMENT;
        if (packageName.equals("Commands.Splatoon2")) return Category.SPLATOON_2;
        if (packageName.equals("Commands.Emotes")) return Category.EMOTES;
        if (packageName.equals("Commands.Interactions")) return Category.INTERACTIONS;
        if (packageName.equals("Commands.External")) return Category.EXTERNAL;
        if (packageName.equals("Commands.osu")) return Category.OSU;
        if (packageName.equals("Commands.PowerPlant")) return Category.POWER_PLANT;
        if (packageName.equals("Commands.Casino")) return Category.CASINO;
        if (packageName.equals("Commands.Moderation")) return Category.MODERATION;
        return null;
    }

    public static String getEmojiOfCategory(DiscordApi api, String category) {
        switch (category) {
            case Category.GENERAL:
                return "\uD83D\uDE4B";
            case Category.NSFW:
                return "\uD83D\uDD1E";
            case Category.BOT_MANAGEMENT:
                return "\uD83E\uDD16";
            case Category.EMOTES:
                return "\uD83D\uDE00";
            case Category.INTERACTIONS:
                return "\uD83D\uDC81";
            case Category.EXTERNAL:
                return "\uD83D\uDCE4";
            case Category.SERVER_MANAGEMENT:
                return "\u2699\uFE0F️";
            case Category.POWER_PLANT:
                return "\uD83D\uDC1F";
            case Category.OSU:
                return "\u270D";
            case Category.CASINO:
                return "\uD83C\uDFB0";
            case Category.SPLATOON_2:
                return Shortcuts.getCustomEmojiByID(api,437258157136543744L).getMentionTag();
            case Category.BOT_OWNER:
                return "\uD83D\uDC77";
            case Category.MODERATION:
                return "\uD83D\uDC77";

                default:
                    return null;
        }
    }
}
