package core.utils;

import constants.Settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

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
        return expandTag(str).stream().anyMatch(tag -> {
            for (String filter : Settings.NSFW_FILTERS) {
                if (tag.matches("(?i).*\\b(?<!-)([^ ]*_|)" + Pattern.quote(filter) + "([ _].*|$)")) {
                    return true;
                }
            }
            for (String strictFilter : Settings.NSFW_STRICT_FILTERS) {
                if (tag.matches("(?i)(.* |^)" + Pattern.quote(strictFilter) + "( .*|$)")) {
                    return true;
                }
            }
            for (String filter : additionalFilter) {
                if (tag.matches("(?i).*\\b(?<!-)([^ ]*_|)" + Pattern.quote(filter) + "([ _].*|$)")) {
                    return true;
                }
            }
            return false;
        });
    }

    public static Set<String> expandTag(String tag) {
        HashSet<String> expandedTags = new HashSet<>();
        expandedTags.add(tag.toLowerCase());
        expandedTags.add(tag.toLowerCase().replaceAll("\\p{Punct}| ", "_").replace("__", "_"));
        expandedTags.add(StringUtil.camelToSnake(tag));
        expandedTags.add(StringUtil.camelToSnake(tag).replaceAll("\\p{Punct}| ", "_").replace("__", "_"));
        return expandedTags;
    }

}
