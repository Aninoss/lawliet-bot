package constants;

import java.util.regex.Pattern;

public interface RegexPatterns {

    Pattern BOORU_AMOUNT_PATTERN = Pattern.compile("\\b\\d{1,6}\\b");
    Pattern DIGIT_REFORMAT_PATTERN = Pattern.compile("\\b[0-9]+[\\s| ]+[0-9]");
    Pattern AMOUNT_FILTER_PATTERN = Pattern.compile("\\b(?<digits>\\d{1,16}([.|,]\\d{1,16})?)[\\s| ]*(?<unit>[\\w|%]*)");
    Pattern MINUTES_PATTERN = Pattern.compile("\\b\\d{1,4}[\\s| ]*m(in(utes?)?)?\\b");
    Pattern HOURS_PATTERN = Pattern.compile("\\b\\d{1,4}[\\s| ]*h(ours?)?\\b");
    Pattern DAYS_PATTERN = Pattern.compile("\\b\\d{1,3}[\\s| ]*d(ays?)?\\b");
    Pattern TEXT_PLACEHOLDER_PATTERN = Pattern.compile("\\{(?<inner>[^}]*)}");
    Pattern TEXT_MULTIOPTION_PATTERN = Pattern.compile("(?<!\\\\)\\[(?<inner>[^]]*)]");

}