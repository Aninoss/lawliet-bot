package core.utils;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;
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
        for (String strictFilter : Settings.NSFW_STRICT_FILTERS) {
            str.append(" -").append(strictFilter.toLowerCase());
        }
        for (String filter : additionalFilter) {
            str.append(" -").append(filter.toLowerCase());
        }
        return str.toString();
    }

    public static boolean stringContainsBannedTags(String str, Set<String> additionalFilter) {
        for (String filter : Settings.NSFW_FILTERS) {
            if (str.matches("(?i).*\\b(?<!-)([^ ]*_|)" + Pattern.quote(filter) + "([ _].*|$)")) {
                return true;
            }
        }
        for (String strictFilter : Settings.NSFW_STRICT_FILTERS) {
            if (str.matches("(?i)(.* |^)" + Pattern.quote(strictFilter) + "( .*|$)")) {
                return true;
            }
        }
        for (String filter : additionalFilter) {
            if (str.matches("(?i).*\\b(?<!-)([^ ]*_|)" + Pattern.quote(filter) + "([ _].*|$)")) {
                return true;
            }
        }
        return false;
    }

}
