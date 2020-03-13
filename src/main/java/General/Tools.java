package General;

import Constants.Language;
import Constants.Settings;
import General.Mention.Mention;
import General.Mention.MentionFinder;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Tools {

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

    public static long filterNumberFromString(String string) {
        StringBuilder numberString = new StringBuilder();
        boolean record = true;
        int i = 0;
        for(char c: string.toCharArray()) {
            if (Character.isDigit(c) && record) numberString.append(c);
            if (c == '<' && string.substring(i).contains(">")) record = false;
            if (c == '>') record = true;
            if (c == '.') break;
            i++;
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

    public static String filterLettersFromString(String string) {
        for(int i = 0; i < 10; i++) {
            string = string.replace(String.valueOf(i), "");
        }
        return string;
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
    }

    public static String cutString(String string, String start, String end) {
        string = cutString(string, start);
        string = string.substring(0, string.indexOf(end));
        return string;
    }

    public static String cutString(String string, String start) {
        string = string.substring(string.indexOf(start) + start.length());
        return string;
    }

    public static String cutSpaces(String string) {
        while (string.length() > 0 && string.charAt(0) == ' ') {
            string = string.substring(1, string.length());
        }

        while (string.length() > 0 && string.charAt(string.length()-1) == ' ') {
            string = string.substring(0, string.length()-1);
        }
        return string;
    }

    public static int pickFullRandom(ArrayList<Integer> usedSlots, int size) {
        Random n = new Random();
        int i;
        do {
            i = n.nextInt(size);
        } while (usedSlots.contains(i));
        usedSlots.add(i);
        if (usedSlots.size() == size)
            usedSlots.remove(0);
        return i;
    }

    public static String randomUpperCase(String answer) {
        StringBuilder sb = new StringBuilder();
        Random n = new Random();
        for (char c : answer.toCharArray()) {
            if (n.nextBoolean()) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static Mention getMentionedString(Locale locale, Message message, String followedString) throws IOException {
        int counted = 0;
        boolean multi = false;
        Server server = message.getServer().get();
        List<User> userList = MentionFinder.getUsers(message, followedString).getList();
        List<Role> roleList = MentionFinder.getRoles(message, followedString).getList();
        StringBuilder sb = new StringBuilder();

        for(User user: userList) {
            sb.append("**").append(user.getDisplayName(server)).append("**, ");
            counted++;
        }

        for(Role role: roleList) {
            sb.append("**").append(role.getName()).append("**, ");
            counted++;
            multi = true;
        }

        if (message.mentionsEveryone() || followedString.contains("everyone")) {
            if (counted == 0) sb.append("**").append(TextManager.getString(locale,TextManager.GENERAL,"everyone_start")).append("**, ");
            else sb.append("**").append(TextManager.getString(locale,TextManager.GENERAL,"everyone_end")).append("**, ");
            counted++;
            multi = true;
        }

        if (counted == 0) return null;
        if (counted > 1) multi = true;

        String string = sb.toString();
        string = string.substring(0,string.length()-2);

        if (string.contains(", ")) string = Tools.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

        return new Mention(string,multi);
    }

    public static Mention getMentionedStringOfUsers(Locale locale, Server server, List<User> userList) throws IOException {
        int counted = 0;
        boolean multi = false;
        StringBuilder sb = new StringBuilder();

        for(User user: userList) {
            sb.append("**").append(user.getDisplayName(server)).append("**, ");
            counted++;
        }

        if (counted == 0) throw new IOException();
        if (counted > 1) multi = true;

        String string = sb.toString();
        string = string.substring(0,string.length()-2);

        if (string.contains(", ")) string = Tools.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

        return new Mention(string, multi);
    }

    public static Mention getMentionedStringOfRoles(Locale locale, List<Role> roleList) throws IOException {
        int counted = 0;
        boolean multi = false;
        StringBuilder sb = new StringBuilder();

        for(Role role: roleList) {
            sb.append("**").append(role.getName()).append("**, ");
            counted++;
        }

        if (counted == 0) throw new IOException();
        if (counted > 1) multi = true;

        String string = sb.toString();
        string = string.substring(0,string.length()-2);

        if (string.contains(", ")) string = Tools.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

        return new Mention(string, multi);
    }

    public static String getBar(double value, int number) {
        //String[] blocks = {"⠀", "▏","▎","▍","▌","▋","▊","▉"};
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
            while ((str.charAt(str.length()-1) == '.' || str.charAt(str.length()-1) == ' ' || str.charAt(str.length()-1) == '\n') && str.length() > 0) str = str.substring(0,str.length()-1);

            str = str + " (…)";
        }
        return str;
    }

    public static String getStringIfNotNull(Object o, String ifNull) {
        if (o == null || o.toString().length() == 0) return ifNull;

        if (o instanceof ServerTextChannel) return ((ServerTextChannel) o).getMentionTag();
        if (o instanceof ServerVoiceChannel) return ((ServerVoiceChannel) o).getName();
        if (o instanceof Role) return ((Role) o).getMentionTag();
        if (o instanceof Emoji) return ((Emoji) o).getMentionTag();
        if (o instanceof Message) return ((Message) o).getIdAsString();
        return o.toString();
    }

    public static boolean canManageRole(Role role) {
        return canManageRole(DiscordApiCollection.getInstance().getYourself(), role);
    }

    public static boolean canManageRole(User user, Role role) {
        Server server = role.getServer();
        if (role.isManaged() || !server.canManageRoles(user)) return false;
        if (server.getOwner().getId() == user.getId()) return true;

        int highestPosition = -1;
        for(Role ownRole: server.getRoles(user)) {
            if (ownRole.getPermissions().getState(PermissionType.MANAGE_ROLES) == PermissionState.ALLOWED || ownRole.getPermissions().getState(PermissionType.ADMINISTRATOR) == PermissionState.ALLOWED) {
                highestPosition = Math.max(highestPosition, ownRole.getPosition());
            }
        }

        return highestPosition > role.getPosition();
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

        return sb.toString().toUpperCase()
                ;
    }

    public static String getEmptyCharacter() {
        return "⠀";
    }

    public static Optional<Role> getRoleByTag(Server server, String tag) {
        String id = tag.substring(3, tag.length() -1);
        return server.getRoleById(id);
    }

    public static CustomEmoji getCustomEmojiByTag(String tag) {
        try {
            String[] tags = tag.split(":");
            if (tags.length == 3) {
                tag = tags[2];
                String id = tag.substring(0, tag.length() - 1);
                Optional<KnownCustomEmoji> customEmojiOptional = DiscordApiCollection.getInstance().getCustomEmojiById(id);
                if (customEmojiOptional.isPresent()) {
                    return customEmojiOptional.get();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean userHasAdminPermissions(Server server, User user) {
        for (Role role : user.getRoles(server)) {
            if (role.getPermissions().getState(PermissionType.ADMINISTRATOR) == PermissionState.ALLOWED || user.getId() == server.getOwner().getId()) {
                return true;
            }
        }
        return false;
    }

    public static URL getURLFromInputStream(InputStream inputStream) throws ExecutionException, InterruptedException {
        Message message = DiscordApiCollection.getInstance().getHomeServer().getTextChannelById(521088289894039562L).get().sendMessage(inputStream, "welcome.png").get();
        URL url = message.getAttachments().get(0).getUrl();
        Thread t =new Thread(() -> {
            try {
                Thread.sleep(10000);
                message.delete();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.setName("message_delete_counter");
        t.start();
        return url;
    }

    public static long getURLFileSize(URL url) {
        long size = 0;
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).setRequestMethod("HEAD");
            }
            conn.getInputStream();
            size = conn.getContentLength();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).disconnect();
            }
        }
        return size;
    }

    public static String solveVariablesOfCommandText(String string, Message message, String prefix) {
        return string
                .replaceAll("(?i)%MessageContent",message.getContent())
                .replaceAll("(?i)%#Channel",message.getServerTextChannel().get().getMentionTag())
                .replaceAll("(?i)%MessageID", message.getIdAsString())
                .replaceAll("(?i)%ChannelID",message.getServerTextChannel().get().getIdAsString())
                .replaceAll("(?i)%ServerID",message.getServer().get().getIdAsString())
                .replaceAll("(?i)%@User",message.getUserAuthor().get().getMentionTag())
                .replaceAll("(?i)%@Bot",DiscordApiCollection.getInstance().getYourself().getMentionTag())
                .replaceAll("(?i)%Prefix",prefix);
    }

    public static String solveVariablesOfCommandText(String string) {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        return string
                .replaceAll("(?i)%MessageContent", "hello")
                .replaceAll("(?i)%#Channel", "#welcome")
                .replaceAll("(?i)%MessageID", "557961653975515168")
                .replaceAll("(?i)%ChannelID", "557953262305804310")
                .replaceAll("(?i)%ServerID", "557953262305804308")
                .replaceAll("(?i)%@User", "@" + apiCollection.getOwner().getDiscriminatedName())
                .replaceAll("(?i)%@Bot", "@" + apiCollection.getYourself().getDiscriminatedName())
                .replaceAll("(?i)%Prefix", "L.");
    }

    public static String getInstantString(Locale locale, Instant instant, boolean withClockTime) throws IOException {
        String str = DateTimeFormatter.ofPattern(TextManager.getString(locale, TextManager.GENERAL, "time_code", withClockTime)).format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
        if (withClockTime) {
            str += " " + TextManager.getString(locale, TextManager.GENERAL, "clock");
        }

        return str;
    }

    public static String getRemainingTimeString(Locale locale, Instant time0, Instant time1, boolean shorter) throws IOException {
        String remaining = "";

        long diff = Math.abs(Date.from(time0).getTime() - Date.from(time1).getTime()) + 1000 * 60;

        int days = (int) (diff / (24 * 60 * 60 * 1000));
        int hours = (int) (diff / (60 * 60 * 1000) % 24);
        int minutes = (int) (diff / (60 * 1000) % 60);

        String addString = "";
        if (shorter) addString = "_shorter";

        if (days > 0) remaining += days + " " + TextManager.getString(locale, TextManager.GENERAL, "days" + addString, days != 1) + ", ";
        if (hours > 0) remaining += hours + " " + TextManager.getString(locale, TextManager.GENERAL, "hours" + addString, hours != 1) + ", ";
        if (minutes > 0) remaining += minutes + " " + TextManager.getString(locale, TextManager.GENERAL, "minutes" + addString, minutes != 1) + ", ";

        remaining = remaining.substring(0, remaining.length() - 2);
        remaining = Tools.replaceLast(remaining, ",", " " + TextManager.getString(locale, TextManager.GENERAL, "and"));
        return remaining;
    }

    public static Instant setInstantToNextHour(Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime roundCeiling = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        return roundCeiling.toInstant(ZoneOffset.UTC);
    }

    public static Instant setInstantToNextDay(Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault());
        LocalDateTime roundCeiling = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        return roundCeiling.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
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

    public static String getMessageURL(Message message) {
        return "https://discordapp.com/channels/"+message.getServer().get().getIdAsString()+"/"+message.getChannel().getIdAsString()+"/"+message.getIdAsString();
    }

    public static boolean UrlContainsImage(String url) {
        return url.endsWith("jpeg") || url.endsWith("jpg") || url.endsWith("png") || url.endsWith("bmp") || url.endsWith("gif");
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
        if (getLanguage(locale) == Language.DE) str = str.replace(",",".");

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
        if (language.equalsIgnoreCase("de")) {
            return Language.DE;
        } else {
            return Language.EN;
        }
    }

    public static String getLoadingReaction(ServerTextChannel channel) {
        if (channel.canYouUseExternalEmojis())
            return DiscordApiCollection.getInstance().getHomeEmojiById(407189379749117981L).getMentionTag();
        else return "⏳";
    }

    public static boolean serverIsBotListServer(Server server) {
        return server.getId() == 264445053596991498L;
    }

    public static String getCurrentVersion() {
        return Settings.VERSIONS[Settings.VERSIONS.length - 1];
    }

    public static String getStats(Locale locale, User user) {
        return TextManager.getString(locale, TextManager.GENERAL, "status_" + user.getStatus().getStatusString());
    }

    public static boolean canSendPrivateMessage(User user) {
        try {
            user.openPrivateChannel().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
        }

        return false;
    }

    public static boolean canYouKickUser(Server server, User user) {
        return server.canYouKickUser(user) && server.getOwner() != user;
    }

    public static boolean canYouBanUser(Server server, User user) {
        return server.canYouBanUser(user) && server.getOwner() != user;
    }

    public static boolean canManageChannel(ServerChannel channel, User user) {
        return channel.getServer().getPermissions(user).getState(PermissionType.ADMINISTRATOR) == PermissionState.ALLOWED || channel.getEffectivePermissions(user).getState(PermissionType.MANAGE_CHANNELS) == PermissionState.ALLOWED;
    }

    public static boolean canManagePermissions(ServerChannel channel, User user) {
        return channel.getServer().getPermissions(user).getState(PermissionType.ADMINISTRATOR) == PermissionState.ALLOWED || channel.getEffectivePermissions(user).getState(PermissionType.MANAGE_ROLES) == PermissionState.ALLOWED;
    }

    public static String decryptString(String str) {
        return Jsoup.parse(str.replace("<br />", "\n")).text();
    }

    public static Instant parseDateString(String str) {
        String[] timeString = str.split(" ");

        int month = 0;
        String monthString = timeString[1];
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < 12; i++) {
            if (monthString.equalsIgnoreCase(monthNames[i])) {
                month = i + 1;
                break;
            }
        }

        LocalDateTime ldt1 = LocalDateTime.now()
                .withYear(Integer.parseInt(timeString[5]))
                .withMonth(month)
                .withDayOfMonth(Integer.parseInt(timeString[2]))
                .withHour(Integer.parseInt(timeString[3].split(":")[0]))
                .withMinute(Integer.parseInt(timeString[3].split(":")[1]))
                .withSecond(Integer.parseInt(timeString[3].split(":")[2]));

        return ldt1.atZone(ZoneOffset.UTC).toInstant();
    }

    public static Instant parseDateString2(String str) {
        String[] timeString = str.split(" ");

        int month = 0;
        String monthString = timeString[2];
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < 12; i++) {
            if (monthString.equalsIgnoreCase(monthNames[i])) {
                month = i + 1;
                break;
            }
        }

        LocalDateTime ldt1 = LocalDateTime.now()
                .withYear(Integer.parseInt(timeString[3]))
                .withMonth(month)
                .withDayOfMonth(Integer.parseInt(timeString[1]))
                .withHour(Integer.parseInt(timeString[4].split(":")[0]))
                .withMinute(Integer.parseInt(timeString[4].split(":")[1]))
                .withSecond(Integer.parseInt(timeString[4].split(":")[2]));

        return ldt1.atZone(ZoneOffset.UTC).toInstant();
    }

    public static String filterPornSearchKey(String str, ArrayList<String> additionalFilter) {
        for(String filter: Settings.NSFW_FILTERS) {
            str = str.replace(filter, "");
        }
        for(String filter: additionalFilter) {
            str = str.replace(filter, "");
        }
        return str;
    }

    public static String getNSFWTagRemoveList(ArrayList<String> additionalFilter) {
        StringBuilder str = new StringBuilder();
        for(String filter: Settings.NSFW_FILTERS) {
            str.append(" -").append(filter);
        }
        for(String filter: additionalFilter) {
            str.append(" -").append(filter);
        }
        return str.toString();
    }

    public static boolean stringContainsBannedTags(String str, ArrayList<String> additionalFilter) {
        return !(filterPornSearchKey(str, additionalFilter).equalsIgnoreCase(str));
    }

    public static String defuseAtEveryone(String str) {
        return str.replace("@everyone", "@\u200Beveryone").replace("@here", "@\u200Bhere");
    }

    public static long getAmountExt(String str, long available) {
        if (str.toLowerCase().contains("all")) return available;
        if (str.toLowerCase().contains("half")) return available / 2;

        long value = Tools.filterNumberFromString(str);
        if (value == -1) return -1;
        return str.contains("%") ? (long)(value / 100.0 * available) : value;
    }

}