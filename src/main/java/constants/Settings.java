package constants;

import java.awt.*;

public interface Settings {

    int TIME_OUT_TIME = 10 * 60 * 1000;
    long FISHERY_MAX = 9999999999999999L;
    String[] NSFW_FILTERS = { "shota", "loli", "bestiality", "beastiality", "cub", "vore", "gore", "scat", "rape" };
    int UPDATE_HOUR = 8;
    int COOLDOWN_TIME_SEC = 4;
    long[] PATREON_ROLE_IDS = { 703303395867492453L, 704721905453629481L, 704721939968688249L, 706143381784494132L, 706143478085582898L };
    Color PATREON_COLOR = new Color(249, 104, 84);
    boolean GIVEAWAY_RUNNING = false;
    int FISHERY_DESPAWN_MINUTES = 1;
    int TRACKER_SHARDS = 2;

}