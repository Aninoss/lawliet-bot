package core.utils;

import constants.Language;
import core.DiscordApiCollection;
import core.TextManager;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public final class StringUtil {

    private StringUtil() {}

    public static boolean stringIsDouble(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean stringIsLong(String string) {
        try {
            Long.parseLong(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean stringIsInt(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean stringIsLetters(String string) {
        for(char c : string.toCharArray()) {
            if (!Character.isLetter(c))
                return false;
        }

        return true;
    }

    public static long filterLongFromString(String string) {
        StringBuilder numberString = new StringBuilder();

        for(char c: string.replace(",", ".").toCharArray()) {
            if (Character.isDigit(c)) numberString.append(c);
            if (c == '.') break;
        }

        if (numberString.toString().length() == 0) return -1;

        long num = Long.MAX_VALUE;

        try {
            num = Long.parseLong(numberString.toString());
        } catch (Throwable e) {
            //Ignore
        }

        return num;
    }

    public static double filterDoubleFromString(String string) {
        StringBuilder numberString = new StringBuilder();

        for(char c: string.replace(",", ".").toCharArray()) {
            if (Character.isDigit(c)) numberString.append(c);
            if (c == '.') {
                if (numberString.toString().contains(".")) break;
                else numberString.append(".");
            }
        }

        if (numberString.toString().length() == 0) return -1;
        double num = -1;

        try {
            num = Double.parseDouble(numberString.toString());
        } catch (Throwable e) {
            //Ignore
        }

        return num;
    }

    public static String filterDoubleString(String string) {
        StringBuilder numberString = new StringBuilder();
        string = string.replace(",", ".");

        for(char c: string.toCharArray()) {
            if (Character.isDigit(c)) numberString.append(c);
            else if (c == '.') {
                if (numberString.toString().contains(".")) break;
                else numberString.append(".");
            }
            else break;
        }

        String filteredString = numberString.toString();
        if (filteredString.endsWith("."))
            filteredString = filteredString.substring(0, filteredString.length() - 1);

        return filteredString;
    }

    public static String filterLettersFromString(String string) {
        for(int i = 0; i < 10; i++) {
            string = string.replace(String.valueOf(i), "");
        }
        return string;
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
    }

    public static String[] extractGroups(String string, String start, String end) {
        ArrayList<String> groups = new ArrayList<>();
        while(string.contains(start) && string.contains(end))
        {
            int startIndex = string.indexOf(start) + start.length();
            int endIndex = string.indexOf(end, startIndex);
            if (endIndex == -1) break;

            String groupStr = "";
            if (endIndex > startIndex) groupStr = string.substring(startIndex, endIndex);
            groups.add(groupStr);

            string = string.replaceFirst(start, "");
            string = string.replaceFirst(end, "");
        }
        return groups.toArray(new String[0]);
    }

    public static String cutString(String string, String start) {
        string = string.substring(string.indexOf(start) + start.length());
        return string;
    }

    public static String trimString(String string) {
        if (string.isEmpty()) return string;

        while (string.length() > 0 && string.charAt(0) == ' ') {
            string = string.substring(1);
        }

        while (string.length() > 0 && string.charAt(string.length() - 1) == ' ') {
            string = string.substring(0, string.length()-1);
        }
        return string;
    }

    public static String getBar(double value, int number) {
        String[] blocks = {"░","█"};
        double boxes = value*number;
        StringBuilder sb = new StringBuilder();
        for(double i=0; i < Math.ceil(boxes); i++) {
            if (i >= boxes) break;
            int index = (int) Math.min(blocks.length-1,Math.round((boxes-i) * (blocks.length-1)));
            sb.append(blocks[index]);
        }
        while(sb.length()<number) sb.append(blocks[0]);
        return sb.toString();
    }

    public static String shortenString(String str, int limit) {
        return shortenString(str, limit, "…", false);
    }

    public static String shortenStringLine(String str, int limit) {
        return shortenString(str, limit, "\n…", true);
    }

    public static String shortenString(String str, int limit, String postfix, boolean focusLineBreak) {
        if (str.length() <= limit)
            return str;

        while(str.length() > limit - postfix.length() && str.contains("\n")) {
            int pos = str.lastIndexOf("\n");
            str = str.substring(0, pos);
        }

        if (!focusLineBreak) {
            while (str.length() > limit - postfix.length() && str.contains(" ")){
                int pos = str.lastIndexOf(" ");
                str = str.substring(0, pos);
            }
        }

        while (str.length() > 0 && (str.charAt(str.length() - 1) == '.' || str.charAt(str.length() - 1) == ' ' || str.charAt(str.length() - 1) == '\n'))
            str = str.substring(0, str.length() - 1);

        return str.substring(0, Math.min(str.length(), limit - postfix.length())) + postfix;
    }

    public static String getEmojiForBoolean(boolean bool) {
        if (bool) return "✅";
        return "❌";
    }

    public static String getOnOffForBoolean(Locale locale, boolean bool) throws IOException {
        StringBuilder sb = new StringBuilder("**");

        if (bool) sb.append("✅ ");
        else sb.append("❌ ");

        sb.append(TextManager.getString(locale, TextManager.GENERAL, "onoff", bool)).append("**");

        return sb.toString().toUpperCase();
    }

    public static String solveVariablesOfCommandText(String string, Message message, String prefix) {
        return string
                .replaceAll("(?i)%MessageContent",message.getContent())
                .replaceAll("(?i)%#Channel",message.getServerTextChannel().get().getMentionTag())
                .replaceAll("(?i)%MessageID", message.getIdAsString())
                .replaceAll("(?i)%ChannelID",message.getServerTextChannel().get().getIdAsString())
                .replaceAll("(?i)%ServerID",message.getServer().get().getIdAsString())
                .replaceAll("(?i)%@User",message.getUserAuthor().get().getMentionTag())
                .replaceAll("(?i)%@Bot", DiscordApiCollection.getInstance().getYourself().getMentionTag())
                .replaceAll("(?i)%Prefix",prefix);
    }

    public static String doubleToString(double d, int placesAfterPoint) {
        return doubleToString(d, placesAfterPoint, Locale.US);
    }

    public static String doubleToString(double d, int placesAfterPoint, Locale locale) {
        StringBuilder pattern = new StringBuilder("#");
        if (placesAfterPoint > 0) pattern.append(".");
        for(int i=0; i<placesAfterPoint; i++) {
            pattern.append("#");
        }

        DecimalFormat df = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(Locale.US));
        String str = df.format(d);
        switch (StringUtil.getLanguage(locale)) {
            case DE:
                str = str.replace(".", ",");
                break;

            case RU:
                str = str.replace(".", ",");
                break;

            default:
        }

        return str;
    }

    public static boolean stringContainsLetters(String s) {
        for(char c: s.toCharArray()) {
            if (Character.isLetter(c)) return true;
        }
        return false;
    }

    public static boolean stringContainsDigits(String s) {
        for(char c: s.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    public static String numToString(long n) {
        DecimalFormat formatter = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
        return formatter.format(n).replace(",", " ");
    }

    public static String numToString(int n) {
        return numToString((long) n);
    }

    public static Language getLanguage(Locale locale) {
        String language = locale.getLanguage().split("_")[0].toLowerCase();
        switch (language) {
            case "de": return Language.DE;
            case "ru": return Language.RU;
            default: return Language.EN;
        }
    }

    public static String getLoadingReaction(ServerTextChannel channel) {
        if (channel.canYouUseExternalEmojis())
            return DiscordApiCollection.getInstance().getHomeEmojiById(407189379749117981L).getMentionTag();
        else return "⏳";
    }

    public static String decryptString(String str) {
        return Jsoup.parse(str.replace("<br />", "\n")).text();
    }

    public static String defuseMassPing(String str) {
        return str.replace("@everyone", "@\u200Beveryone")
                .replace("@here", "@\u200Bhere")
                .replace("<@&", "<@\u200B&");
    }

    public static String escapeMarkdown(String str) {
        return str.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`")
                .replace("|", "\\|")
                .replace("~", "\\~");
    }

    public static String escapeMarkdownInField(String str) {
        return str.replace("`", "");
    }

    public static String generateHeartBar(int health, int healthMax, boolean lostHealth) {
        StringBuilder sb = new StringBuilder();

        if (health > 0) {
            if (lostHealth) sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729337505626849370L).getMentionTag());
            else sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729332545388544080L).getMentionTag());
        } else sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729337505215938621L).getMentionTag());

        for(int i = 0; i < healthMax; i++) {
            if (i < health) {
                if (lostHealth) sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729338253358137374L).getMentionTag());
                else sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729338714702217256L).getMentionTag());
            } else if (i == health) {
                if (lostHealth) sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729337391369682944L).getMentionTag());
                else sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729338774194225183L).getMentionTag());
            } else {
                sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729338774194225183L).getMentionTag());
            }
        }

        sb.append(DiscordApiCollection.getInstance().getHomeEmojiById(729342822536183839L).getMentionTag());
        return sb.toString();
    }

    public static double similarityIgnoreLength(String s1, String s2) {
        if (s1.length() > s2.length()) s1 = s1.substring(0, s2.length());
        else if (s2.length() > s1.length()) s2 = s2.substring(0, s1.length());

        return similarity(s1, s2);
    }

    public static double similarity(String s1, String s2) {
        String longer = s1;
        String shorter = s2;
        if (s1.length() < s2.length()) {
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0)
            return 1.0;
        if (s1.equalsIgnoreCase(s2))
            return 1.0;

        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    private static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    public static boolean stringContainsVague(String str0, String str1) {
        return str0.toLowerCase().replace(" ", "").contains(str1.toLowerCase().replace(" ", ""));
    }

}