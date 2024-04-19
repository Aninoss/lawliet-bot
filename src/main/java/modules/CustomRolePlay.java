package modules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomRolePlay {

    public static String resolveVariables(String string, String author, String mentions) {
        return string.replaceAll("(?i)" + Pattern.quote("%author"), Matcher.quoteReplacement(author))
                .replaceAll("(?i)" + Pattern.quote("%user_mentions"), Matcher.quoteReplacement(mentions))
                .replaceAll("(?i)" + Pattern.quote("%user\\_mentions"), Matcher.quoteReplacement(mentions));
    }

}
