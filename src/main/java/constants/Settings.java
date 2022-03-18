package constants;

import java.awt.*;

public interface Settings {

    int TIME_OUT_MINUTES = 10;
    long FISHERY_MAX = 9_999_999_999_999_999L;
    int FISHERY_GEAR_MAX = 999_999;
    int FISHERY_SHARES_MAX = 99_999_999;
    int FISHERY_SHARES_FEES = 4;
    String[] NSFW_FILTERS = { "shota", "loli", "bestiality", "beastiality", "zoophilia", "cub", "vore", "gore", "guro", "scat", "rape", "necrophilia", "corpse", "children", "child", "kid", "kids", "teenager", "young", "underage", "underaged" };
    int COOLDOWN_TIME_SEC = 30;
    int COOLDOWN_MAX_ALLOWED = 5;
    long[] PATREON_ROLE_IDS = { 762322081840234506L, 703303395867492453L, 704721905453629481L, 704721939968688249L, 706143381784494132L, 706143478085582898L };
    Color PREMIUM_COLOR = new Color(42, 127, 239);
    int FISHERY_DESPAWN_MINUTES = 1;

}