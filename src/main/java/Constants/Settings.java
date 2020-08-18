package Constants;

import java.awt.*;

public interface Settings {

    long LAWLIET_ID = 368521195940741122L;
    int TIME_OUT_TIME = 10 * 60 * 1000;
    long HOME_SERVER_ID = 368531164861825024L;
    long SUPPORT_SERVER_ID = 557953262305804308L;
    String BOT_INVITE_URL = "https://lawlietbot.xyz/invite?BOT";
    String BOT_INVITE_REMINDER_URL = "https://lawlietbot.xyz/invite?BOT_REMINDER";
    String SERVER_INVITE_URL = "https://discord.gg/F4FcAbQ";
    String DONATION_URL = "https://donatebot.io/checkout/" + SUPPORT_SERVER_ID;
    String UPVOTE_URL = "https://top.gg/bot/368521195940741122/vote";
    String LAWLIET_WEBSITE = "https://lawlietbot.xyz/";
    String FEEDBACK_WEBSITE = "https://lawlietbot.xyz/feedback/%d";
    String PATREON_PAGE = "https://www.patreon.com/lawlietbot";
    String CURRENCY = "\uD83D\uDC1F";
    String COINS = "<a:coin:512684910620835841>";
    String GROWTH = "<:growth:556164492678004741>";
    String DAILY_STREAK = "\uD83D\uDD25";
    long MAX = 9999999999999999L;
    String BACK_EMOJI = "⏪";
    String EMPTY_EMOJI = "⠀";
    String[] NSFW_FILTERS = {"shota", "loli", "bestiality", "beastiality", "cub", "vore", "gore", "scat", "rape"};
    int UPDATE_HOUR = 7;
    int COOLDOWN_TIME_SEC = 4;
    long[] DONATION_ROLE_IDS = { 703303395867492453L, 704721905453629481L, 704721939968688249L, 706143381784494132L, 706143478085582898L };
    Color PATREON_COLOR = new Color(249, 104, 84);
    boolean GIVEAWAY_RUNNING = false;
    int FISHERY_DESPAWN_MINUTES = 5;

    String[] VERSIONS = {
            "2.0.0",
            "2.1.0",
            "2.2.0",
            "2.3.0", "2.3.1", "2.3.2",
            "2.4.0", "2.4.1",
            "2.5.0", "2.5.1", "2.5.2", "2.5.3", "2.5.4", "2.5.5", "2.5.6", "2.5.7",
            "2.6.0", "2.6.1", "2.6.2", "2.6.3", "2.6.4", "2.6.5", "2.6.6", "2.6.7",
            "2.7.0", "2.7.1", "2.7.2", "2.7.3", "2.7.4",
            "2.8.0", "2.8.1", "2.8.2", "2.8.3", "2.8.4", "2.8.5", "2.8.6", "2.8.7", "2.8.8", "2.8.9", "2.8.10", "2.8.11",
            "2.9.1", "2.9.2", "2.9.3", "2.9.4", "2.9.5", "2.9.6", "2.9.7",
            "2.10.0", "2.10.1", "2.10.2", "2.10.3", "2.10.4", "2.10.5", "2.10.6", "2.10.7", "2.10.8", "2.10.9",
            "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7",
            "2.12.0", "2.12.1", "2.12.2", "2.12.3", "2.12.4", "2.12.5", "2.12.6", "2.12.7", "2.12.8", "2.12.9", "2.12.10", "2.12.11",
            "2.13.1", "2.13.2", "2.13.3", "2.13.4", "2.13.5", "2.13.6", "2.13.7", "2.13.8", "2.13.9", "2.13.10", "2.13.11",
            "2.14.1", "2.14.2", "2.14.3", "2.14.4", "2.14.5", "2.14.6", "2.14.7", "2.14.8", "2.14.9", "2.14.10", "2.14.11", "2.14.12", "2.14.13", "2.14.14", "2.14.15", "2.14.16", "2.14.17", "2.14.18", "2.14.19", "2.14.20",
            "2.15.1", "2.15.2", "2.15.3", "2.15.4", "2.15.5", "2.15.6", "2.15.7", "2.15.8", "2.15.9", "2.15.10", "2.15.11", "2.15.12", "2.15.13"
    };
}