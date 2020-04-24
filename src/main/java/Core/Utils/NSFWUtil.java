package Core.Utils;

import Constants.Settings;

import java.util.ArrayList;

public final class NSFWUtil {

    private NSFWUtil() {}

    public static String filterPornSearchKey(String str, ArrayList<String> additionalFilter) {
        for(String filter: Settings.NSFW_FILTERS) {
            str = str.replace(filter, "");
        }
        for(String filter: additionalFilter) {
            str = str.replace(filter, "");
        }
        return str;
    }

    public static String getNSFWTagRemoveList(ArrayList<String> additionalFilter) {
        StringBuilder str = new StringBuilder();
        for(String filter: Settings.NSFW_FILTERS) {
            str.append(" -").append(filter);
        }
        for(String filter: additionalFilter) {
            str.append(" -").append(filter);
        }
        return str.toString();
    }

    public static boolean stringContainsBannedTags(String str, ArrayList<String> additionalFilter) {
        return !(filterPornSearchKey(str, additionalFilter).equalsIgnoreCase(str));
    }

}
