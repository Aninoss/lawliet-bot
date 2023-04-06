package modules.txt2img;

import java.time.LocalDate;
import java.util.HashMap;

public class Txt2ImgCallTracker {

    private static final HashMap<Long, Integer> callMap = new HashMap<>();
    private static LocalDate lastCallDate = LocalDate.now();

    public static int getCalls(long guildId, long userId) {
        resetCallMapOnNewDay();
        return callMap.getOrDefault(guildId + userId, 0);
    }

    public static void increaseCalls(long guildId, long userId) {
        int calls = getCalls(guildId, userId);
        callMap.put(guildId + userId, calls + 1);
    }

    private static void resetCallMapOnNewDay() {
        if (LocalDate.now().isAfter(lastCallDate)) {
            lastCallDate = LocalDate.now();
            callMap.clear();
        }
    }

}
