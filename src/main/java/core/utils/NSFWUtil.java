package core.utils;

import constants.Settings;

import java.util.ArrayList;
import java.util.regex.Pattern;

public final class NSFWUtil {

    private NSFWUtil() {}

    public static String filterPornSearchKey(String str, ArrayList<String> additionalFilter) {
        for(String filter: Settings.NSFW_FILTERS) {
            str = str.replaceAll("\\b" + Pattern.quote(filter) + "\\b", "");
        }
        for(String filter: additionalFilter) {
            str = str.replaceAll("\\b" + Pattern.quote(filter) + "\\b", "");
        }
        return str;
    }

    public static String getNSFWTagRemoveList(ArrayList<String> additionalFilter) {
        StringBuilder str = new StringBuilder();
        additionalFilter.sort(String::compareTo);
        for(String filter: Settings.NSFW_FILTERS) {
            str.append(" -").append(filter.toLowerCase());
        }
        for(String filter: additionalFilter) {
            str.append(" -").append(filter.toLowerCase());
        }
        return str.toString();
    }

    public static boolean stringContainsBannedTags(String str, ArrayList<String> additionalFilter) {
        return !(filterPornSearchKey(str, additionalFilter).equalsIgnoreCase(str));
    }

}
