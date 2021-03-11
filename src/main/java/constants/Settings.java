package constants;

import java.awt.*;

public interface Settings {

    int TIME_OUT_MINUTES = 10;
    long FISHERY_MAX = 9999999999999999L;
    String[] NSFW_FILTERS = { "shota", "loli", "bestiality", "beastiality", "cub", "vore", "gore", "scat", "rape" };
    int RESTART_HOUR = 9;
    int COOLDOWN_TIME_SEC = 6;
    int COOLDOWN_MAX_ALLOWED = 2;
    long[] PATREON_ROLE_IDS = { 762322081840234506L, 703303395867492453L, 704721905453629481L, 704721939968688249L, 706143381784494132L, 706143478085582898L };
    Color PATREON_COLOR = new Color(249, 104, 84);
    int FISHERY_DESPAWN_MINUTES = 1;

}