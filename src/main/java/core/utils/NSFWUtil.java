package core.utils;

import constants.Settings;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class NSFWUtil {

    private NSFWUtil() {
    }

    public static String generateFilterString(ArrayList<String> additionalFilter) {
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

    public static boolean containsFilterTags(String tagString, Set<String> additionalFilters) {
        List<String> filterTags = new ArrayList<>(List.of(Settings.NSFW_FILTERS));
        filterTags.addAll(additionalFilters);

        return containsNormalFilterTags(tagString, filterTags) ||
                containsStrictFilters(tagString, List.of(Settings.NSFW_STRICT_FILTERS));
    }

    private static boolean containsNormalFilterTags(String tagString, List<String> filterTags) {
        StringBuilder regexBuilder = new StringBuilder("(?i)(^|.* )(|[^-\\P{Punct}]|[^- ][^ ]*\\p{Punct})(");
        for (int i = 0; i < filterTags.size(); i++) {
            if (i > 0) {
                regexBuilder.append("|");
            }
            regexBuilder.append(Pattern.quote(filterTags.get(i)));
        }
        regexBuilder.append(")(( |\\p{Punct}).*|$)");

        return tagString.matches(regexBuilder.toString());
    }

    private static boolean containsStrictFilters(String tagString, List<String> strictFilterTags) {
        String newTagString = " " + tagString.toLowerCase() + " ";
        return strictFilterTags.stream()
                .anyMatch(t -> newTagString.contains(" " + t.toLowerCase() + " "));
    }

    public static String expandTags(String tagString) {
        HashSet<String> expandedTags = new HashSet<>();
        for (String tag : tagString.split(" ")) {
            expandedTags.add(tag.toLowerCase());
            expandedTags.add(StringUtil.camelToSnake(tag));
        }
        return StringUtils.join(expandedTags, " ");
    }

}
