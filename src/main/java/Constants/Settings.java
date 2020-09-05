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
    String FEATURE_REQUESTS_WEBSITE = "https://lawlietbot.xyz/featurerequests";
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
    int TRACKER_SHARDS = 2;

}