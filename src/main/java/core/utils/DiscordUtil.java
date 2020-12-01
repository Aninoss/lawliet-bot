package core.utils;

import core.cache.PatternCache;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordUtil {

    public static ArrayList<String> filterServerInviteLinks(String string) {
        ArrayList<String> list = new ArrayList<>();

        Pattern p = PatternCache.getInstance().generate("(discord\\.gg|discord\\.com\\/invite|discordapp\\.com\\/invite)\\/[A-Za-z0-9_-]*");
        Matcher m = p.matcher(string);
        while (m.find()) {
            list.add(m.group());
        }
        return list;
    }

}
