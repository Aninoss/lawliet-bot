package constants;

import java.util.regex.Pattern;

public interface RegexPatterns {

    Pattern BOORU_NUMBER = Pattern.compile("(^| )[0-9]{1,3}( |$)");
    Pattern DIGIT_REFORMAT = Pattern.compile("\\b[0-9]+[\\s| ]+[0-9]");
    Pattern AMOUNT_FILTER = Pattern.compile("\\b(?<digits>\\d{1,16}([.|,]\\d{1,16})?)[\\s| ]*(?<unit>[\\w|%]*)");
    Pattern MINUTES = Pattern.compile("\\b\\d{1,4}[\\s| ]*m(in(utes?)?)?\\b");
    Pattern HOURS = Pattern.compile("\\b\\d{1,4}[\\s| ]*h(ours?)?\\b");
    Pattern DAYS = Pattern.compile("\\b\\d{1,3}[\\s| ]*d(ays?)?\\b");
    Pattern TEXT_PLACEHOLDER = Pattern.compile("\\{(?<inner>[^}]*)}");
    Pattern TEXT_MULTIOPTION = Pattern.compile("(?<!\\\\)\\[(?<inner>[^]]*)]");
    Pattern EMOTE = Pattern.compile("<a?:(?<name>[^:]*):(?<id>[0-9]*)>");
    Pattern INTERACTION = Pattern.compile("^/api/v[0-9]*/(interactions|webhooks)/.*");
    Pattern SUBREDDIT = Pattern.compile("^(r/)?(?<subreddit>[a-zA-Z0-9-_]*)(/(?<orderby>hot|new|top|controversial))?$");
    Pattern TWITCH = Pattern.compile("^[a-zA-Z0-9][\\w]{2,24}$");
    Pattern HTTP_DOMAIN = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}/?");
    Pattern YOUTUBE_HANDLE = Pattern.compile("@[\\w-]+");

}