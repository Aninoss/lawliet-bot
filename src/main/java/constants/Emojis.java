package constants;

import core.utils.EmojiUtil;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

public interface Emojis {

    UnicodeEmoji CHECKMARK = Emoji.fromUnicode("✅");
    UnicodeEmoji X = Emoji.fromUnicode("❌");
    UnicodeEmoji LIKE = Emoji.fromUnicode("👍");
    UnicodeEmoji DISLIKE = Emoji.fromUnicode("👎");
    UnicodeEmoji[] LETTERS = EmojiUtil.getMultipleFromUnicode(new String[] { "🇦", "🇧", "🇨", "🇩", "🇪", "🇫", "🇬", "🇭", "🇮", "🇯", "🇰", "🇱", "🇲", "🇳", "🇴", "🇵", "🇶", "🇷", "🇸", "🇹", "🇺", "🇻", "🇼", "🇽", "🇾", "🇿" });
    CustomEmoji[] SWITCHES = { Emoji.fromCustom("off", 876483971192602645L, false), Emoji.fromCustom("on", 876483971800760330L, false) };
    CustomEmoji[] SWITCHES_DOT = { Emoji.fromCustom("off", 876483971192602645L, false), Emoji.fromCustom("on_dot", 876484556490956910L, false) };

    CustomEmoji LOADING = Emoji.fromCustom("loading", 407189379749117981L, true);
    UnicodeEmoji LOADING_UNICODE = Emoji.fromUnicode("⏳");
    UnicodeEmoji ZERO_WIDTH_SPACE = Emoji.fromUnicode("‎");
    UnicodeEmoji FULL_SPACE_UNICODE = Emoji.fromUnicode("⠀");
    CustomEmoji FULL_SPACE_EMOTE = Emoji.fromCustom("_", 417016019622559755L, false);

    UnicodeEmoji FISH = Emoji.fromUnicode("🐟");
    CustomEmoji COINS = Emoji.fromCustom("coin", 512684910620835841L, true);
    UnicodeEmoji COINS_UNICODE = Emoji.fromUnicode("🪙");
    UnicodeEmoji GROWTH = Emoji.fromUnicode("⚡");
    UnicodeEmoji DAILY_STREAK = Emoji.fromUnicode("🔥");
    UnicodeEmoji COUPONS = Emoji.fromUnicode("🎟️");

    CustomEmoji COUNTDOWN_10 = Emoji.fromCustom("countdown_10", 729371766119727124L, true);
    CustomEmoji COUNTDOWN_3 = Emoji.fromCustom("countdown_3", 799006900753530880L, true);
    CustomEmoji[] CARD = {
            Emoji.fromCustom("card_0", 729350488390369323L, false),
            Emoji.fromCustom("card_1", 729350405804261428L, false),
            Emoji.fromCustom("card_2", 729350861570179102L, false),
            Emoji.fromCustom("card_3", 729351576841617408L, false),
            Emoji.fromCustom("card_4", 729352445699817493L, false),
            Emoji.fromCustom("card_5", 729353027626205245L, false),
            Emoji.fromCustom("card_6", 729353490521915454L, false),
            Emoji.fromCustom("card_7", 729353805728055298L, false),
            Emoji.fromCustom("card_8", 729354223812083764L, false),
            Emoji.fromCustom("card_9", 729354620828385323L, false),
            Emoji.fromCustom("card_10", 729355033896026194L, false),
            Emoji.fromCustom("card_11", 729355485299605515L, false),
            Emoji.fromCustom("card_12", 729355911805534261L, false)
    };
    CustomEmoji[] CARD_FADEIN = {
            Emoji.fromCustom("card_0_get", 729349470940299314L, true),
            Emoji.fromCustom("card_1_get", 729350272983367702L, true),
            Emoji.fromCustom("card_2_get", 729351437334872117L, true),
            Emoji.fromCustom("card_3_get", 729352020305248266L, true),
            Emoji.fromCustom("card_4_get", 729352658384584745L, true),
            Emoji.fromCustom("card_5_get", 729353271721852998L, true),
            Emoji.fromCustom("card_6_get", 729353661301653604L, true),
            Emoji.fromCustom("card_7_get", 729353963790663739L, true),
            Emoji.fromCustom("card_8_get", 729354396550430840L, true),
            Emoji.fromCustom("card_9_get", 729354763098914897L, true),
            Emoji.fromCustom("card_10_get", 729355024928604182L, true),
            Emoji.fromCustom("card_11_get", 729355637850505256L, true),
            Emoji.fromCustom("card_12_get", 729356087064526899L, true)
    };

    CustomEmoji TOWER_GRAS = Emoji.fromCustom("grass", 734843199985811556L, false);
    CustomEmoji[] TOWER_BASE_BROKEN = { Emoji.fromCustom("tower_base_broken0", 734842201254920223L, false), Emoji.fromCustom("tower_base_broken1", 734842200755535912L, false) };
    CustomEmoji[] TOWER_BASE = { Emoji.fromCustom("tower_base0", 734836799003688981L, false), Emoji.fromCustom("tower_base1", 734836799402409986L, false) };
    CustomEmoji[] TOWER_BASE_FALLING = { Emoji.fromCustom("tower_base0_falling", 735259827563135030L, true), Emoji.fromCustom("tower_base1_falling", 735259827382779985L, true) };

    CustomEmoji SLOT_SPINNING = Emoji.fromCustom("slot", 401057220114251787L, true);
    CustomEmoji SLOT_DR = Emoji.fromCustom("slotdr", 538692177157423115L, false);
    CustomEmoji SLOT_LR = Emoji.fromCustom("slotlr", 538692176998039603L, false);
    CustomEmoji SLOT_DLR = Emoji.fromCustom("slotdlr", 538692177321000960L, false);
    CustomEmoji SLOT_DL = Emoji.fromCustom("slotdl", 538692177019273238L, false);
    CustomEmoji SLOT_UD = Emoji.fromCustom("slotud", 538692176910090272L, false);
    CustomEmoji SLOT_UR = Emoji.fromCustom("slotur", 538692177077731351L, false);
    CustomEmoji SLOT_ULR = Emoji.fromCustom("slotulr", 538692177413275648L, false);
    CustomEmoji SLOT_UL = Emoji.fromCustom("slotul", 538692177400823818L, false);
    CustomEmoji SLOT_LR1 = Emoji.fromCustom("slotlr1", 538709492628586516L, false);
    CustomEmoji SLOT_LR2 = Emoji.fromCustom("slotlr2", 538709122778923038L, false);
    CustomEmoji SLOT_LR3 = Emoji.fromCustom("slotlr3", 538709443068690437L, false);

    CustomEmoji COMMAND_ICON_LOCKED = Emoji.fromCustom("icon_locked", 652188097911717910L, false);
    CustomEmoji COMMAND_ICON_ALERTS = Emoji.fromCustom("icon_tracker", 654051035249115147L, false);
    CustomEmoji COMMAND_ICON_NSFW = Emoji.fromCustom("icon_nsfw", 652188472295292998L, false);
    CustomEmoji COMMAND_ICON_PREMIUM = Emoji.fromCustom("icon_premium", 905196889216024597L, false);

    CustomEmoji SPLATOON_REGULAR = Emoji.fromCustom("regular", 400445711486943235L, false);
    CustomEmoji SPLATOON_LEAGUE = Emoji.fromCustom("league", 400445711432286209L, false);
    CustomEmoji SPLATOON_GACHI = Emoji.fromCustom("gachi", 400445711654584321L, false);
    CustomEmoji SPLATOON_SALMONRUN = Emoji.fromCustom("salmonrun", 400461201177575425L, false);
    CustomEmoji SPLATOON_SQUID = Emoji.fromCustom("squid", 437258157136543744L, false);
    CustomEmoji SPLATOON_SPLATFEST = Emoji.fromCustom("splatfest", 747118860313821214L, false);

    CustomEmoji HEART_NORMAL = Emoji.fromCustom("heart_normal", 729332545388544080L, false);
    CustomEmoji HEART_HIT_GAMEOVER = Emoji.fromCustom("heart_hit_gameover", 729337505215938621L, true);
    CustomEmoji HEART_HIT = Emoji.fromCustom("heart_hit", 729337505626849370L, true);
    CustomEmoji HEART_BAR_HIT_LOSE = Emoji.fromCustom("heart_bar_hit_lose", 729337391369682944L, true);
    CustomEmoji HEART_BAR_HIT = Emoji.fromCustom("heart_bar_hit", 729338253358137374L, true);
    CustomEmoji HEART_BAR_EMPTY = Emoji.fromCustom("heart_bar_empty", 729338774194225183L, false);
    CustomEmoji HEART_BAR_BORDER = Emoji.fromCustom("heart_bar_border", 729342822536183839L, false);
    CustomEmoji HEART_BAR = Emoji.fromCustom("heart_bar", 729338714702217256L, false);

    CustomEmoji POWERUP_SHIELD = Emoji.fromCustom("powerup_shield", 1077261349463269440L, false);
    CustomEmoji POWERUP_LOUPE = Emoji.fromCustom("powerup_loupe", 1077261347752005783L, false);
    CustomEmoji POWERUP_SHOP = Emoji.fromCustom("powerup_shop", 1077261351275204698L, false);
    CustomEmoji POWERUP_TEAM = Emoji.fromCustom("powerup_team", 1077261352722235442L, false);

    CustomEmoji[] STEP_POINTS = { Emoji.fromCustom("step_point_inactive", 1326927792386084894L, false), Emoji.fromCustom("step_point_active", 1326927790888456233L, false) };

}
