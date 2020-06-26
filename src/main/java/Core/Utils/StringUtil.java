package Core.Utils;

import Constants.Language;
import Constants.Settings;
import Core.DiscordApiCollection;
import Core.TextManager;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.Message;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

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
        ArrayList<String> groups = new ArrayList<String>();
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
        if (str.length() > limit) {
            str = str.substring(0, limit - 4);

            if (str.contains("\n")) {
                int pos = str.lastIndexOf("\n");
                str = str.substring(0, pos);
            } else {
                if (str.contains(" ")) {
                    int pos = str.lastIndexOf(" ");
                    str = str.substring(0, pos);
                }
            }
            while (str.length() > 0 && (str.charAt(str.length() - 1) == '.' || str.charAt(str.length() - 1) == ' ' || str.charAt(str.length() - 1) == '\n')) str = str.substring(0, str.length() - 1);

            str = str + " (…)";
        }
        return str;
    }

    public static String shortenStringLine(String str, int limit) {
        if (str.length() > limit) {
            str = str.substring(0, limit - 4);

            if (str.contains("\n")) {
                int pos = str.lastIndexOf("\n");
                str = str.substring(0, pos);
            }
            while ((str.charAt(str.length()-1) == '.' || str.charAt(str.length()-1) == ' ' || str.charAt(str.length()-1) == '\n') && str.length() > 0) str = str.substring(0,str.length()-1);

            str = str + "\n(…)";
        }
        return str;
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

    public static String solveVariablesOfCommandText(String string) {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        return string
                .replaceAll("(?i)%MessageContent", "hello")
                .replaceAll("(?i)%#Channel", "#welcome")
                .replaceAll("(?i)%MessageID", "557961653975515168")
                .replaceAll("(?i)%ChannelID", "557953262305804310")
                .replaceAll("(?i)%ServerID", String.valueOf(Settings.SUPPORT_SERVER_ID))
                .replaceAll("(?i)%@User", "@" + apiCollection.getOwner().getDiscriminatedName())
                .replaceAll("(?i)%@Bot", "@" + apiCollection.getYourself().getDiscriminatedName())
                .replaceAll("(?i)%Prefix", "L.");
    }

    public static String doubleToString(double d, int placesAfterPoint) {
        String pattern = "#";
        if (placesAfterPoint > 0) pattern += ".";
        for(int i=0; i<placesAfterPoint; i++) {
            pattern += "#";
        }
        DecimalFormat df = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(d);
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

    public static String numToString(Locale locale, long n) {
        DecimalFormat formatter = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
        String str = formatter.format(n);

        switch (getLanguage(locale)) {
            case RU:
            case DE: str = str.replace(",","."); break;
        }

        return str;
    }

    public static String numToString(Locale locale, int n) {
        return numToString(locale, (long) n);
    }

    public static String numToString(long n) {
        return numToString(Locale.US, n);
    }

    public static String numToString(int n) {
        return numToString((long) n);
    }

    public static Language getLanguage(Locale locale) {
        String language = locale.getLanguage().split("_")[0].toLowerCase();
        switch (locale.getLanguage().split("_")[0].toLowerCase()) {
            case "de": return Language.DE;
            case "ru": return Language.RU;
            default: return Language.DE;
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
        return str.replace("@everyone", "@\u200Beveryone").replace("@here", "@\u200Bhere").replace("<@&", "<@\u200B&");
    }

    public static String removeMarkdown(String str) {
        return str.replace("`", "")
                .replace("*", "")
                .replace("_", "")
                .replace("~", "")
                .replace("|", "");
    }

}