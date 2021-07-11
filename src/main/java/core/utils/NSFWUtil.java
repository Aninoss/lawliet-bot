package core.utils;

import java.util.ArrayList;
import constants.Settings;

public final class NSFWUtil {

    private NSFWUtil() {
    }

    public static String getNSFWTagRemoveList(ArrayList<String> additionalFilter) {
        StringBuilder str = new StringBuilder();
        additionalFilter.sort(String::compareTo);
        for (String filter : Settings.NSFW_FILTERS) {
            str.append(" -").append(filter.toLowerCase());
        }
        for (String filter : additionalFilter) {
            str.append(" -").append(filter.toLowerCase());
        }
        return str.toString();
    }

}
