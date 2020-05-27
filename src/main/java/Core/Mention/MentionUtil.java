package Core.Mention;

import Core.DiscordApiCollection;
import Core.Utils.StringUtil;
import Core.TextManager;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class MentionUtil {

    final static Logger LOGGER = LoggerFactory.getLogger(MentionUtil.class);

    public static MentionList<User> getUsers(Message message, String content) {
        ArrayList<User> list = new ArrayList<>(message.getMentionedUsers());
        if (!content.contains(DiscordApiCollection.getInstance().getYourself().getIdAsString())) list.remove(DiscordApiCollection.getInstance().getYourself());
        list.removeIf(user -> !message.getServer().get().getMembers().contains(user));

        for (User user : list)
            content = content
                    .replace(user.getMentionTag(), "")
                    .replace("<@!" + user.getIdAsString() + ">", "");

        for(User user : message.getServer().get().getMembers()) {
            String[] strings = {
                    "@" + user.getDiscriminatedName(),
                    "@" + user.getName(),
                    "@" + user.getDisplayName(message.getServer().get()) + "#" + user.getDiscriminator(),
                    "@" + user.getDisplayName(message.getServer().get()),
                    user.getIdAsString(),
                    user.getDiscriminatedName(),
                    user.getName(),
                    user.getDisplayName(message.getServer().get())
            };
            content = check(user, list, content, strings);
        }

        content = StringUtil.trimString(content);
        return new MentionList<>(content, list);
    }

    public static MentionList<Role> getRoles(Message message, String content) {
        ArrayList<Role> list = new ArrayList<>(message.getMentionedRoles());
        list.removeIf(role -> !message.getServer().get().getRoles().contains(role));

        for (Role role : list)
            content = content.replace(role.getMentionTag(), "");

        for(Role role : message.getServer().get().getRoles()) {
            String[] strings = {
                    "@" + role.getName(),
                    role.getName(),
                    role.getIdAsString()
            };
            content = check(role, list, content, strings);
        }

        content = StringUtil.trimString(content);
        return new MentionList<>(content, list);
    }

    public static MentionList<ServerTextChannel> getTextChannels(Message message, String content) {
        ArrayList<ServerTextChannel> list = new ArrayList<>(message.getMentionedChannels());
        list.removeIf(channel -> !message.getServer().get().getTextChannels().contains(channel));

        for (ServerTextChannel channel : list)
            content = content.replace(channel.getMentionTag(), "");

        for(ServerTextChannel channel : message.getServer().get().getTextChannels()) {
            String[] strings = {
                    "#" + channel.getName(),
                    channel.getName(),
                    channel.getIdAsString()
            };
            content = check(channel, list, content, strings);
        }

        content = StringUtil.trimString(content);
        return new MentionList<>(content, list);
    }

    public static MentionList<ServerVoiceChannel> getVoiceChannels(Message message, String content) {
        ArrayList<ServerVoiceChannel> list = new ArrayList<>();

        for(ServerVoiceChannel voiceChannel : message.getServer().get().getVoiceChannels()) {
            String[] strings = {
                    "#" + voiceChannel.getName(),
                    voiceChannel.getName(),
                    voiceChannel.getIdAsString()
            };
            content = check(voiceChannel, list, content, strings);
        }

        content = StringUtil.trimString(content);
        return new MentionList<>(content, list);
    }

    private static <T> String check(T o, ArrayList<T> list, String content, String... names) {
        for(String name : names) {
            if (matches(content, name)) {
                content = content.replaceAll("(?i)" + name, "");
                if (!list.contains(o)) list.add(o);
            }
        }

        return content;
    }

    private static boolean matches(String str, String check) {
        check = check.toLowerCase();
        str = str.toLowerCase().replace("\n", " ");

        return  str.equals(check) ||
                str.startsWith(check + " ") ||
                str.contains(" " + check + " ") ||
                str.endsWith(" " + check);
    }

    public static MentionList<URL> getImages(String string) {
        ArrayList<URL> list = new ArrayList<>();

        for (String part : getArgs(string)) {
            if (urlContainsImage(part)) {
                if (!part.contains(" ") && !part.contains("\n")) {
                    try {
                        URL urlTemp = new URL(part);
                        if (!list.contains(urlTemp)) list.add(urlTemp);
                        string = string.replace(part, "");
                    } catch (MalformedURLException e) {
                        LOGGER.error("Wrong url", e);
                    }
                }
            }
        }

        return new MentionList<>(string, list);
    }

    private static boolean urlContainsImage(String url) {
        String fileType = "";
        try {
            URLConnection conn = new URL(url).openConnection();
            if (conn == null) return false;
            fileType = conn.getContentType().toLowerCase();

            for(int i = 0; i < 2; i++) {
                if (fileType.endsWith("jpg") || fileType.endsWith("jpeg") || fileType.endsWith("png") || fileType.endsWith("bmp")) return true;
                fileType = url.toLowerCase();
            }

            return false;
        } catch (IOException e) {
            //Ignore
        }
        return false;
    }

    public static MentionList<Message> getMessagesAll(Message message, String string) {
        MentionList<Message> mentionList = getMessagesURL(message, string);
        ArrayList<Message> list = new ArrayList<>(mentionList.getList());
        string = mentionList.getResultMessageString();

        for (String part : getArgs(string)) {
            for (ServerTextChannel channel : message.getServer().get().getTextChannels()) {
                Message m;
                try {
                    if (StringUtil.stringIsLong(part) && (m = channel.getMessageById(part).get()) != null) {
                        if (m.getChannel() == channel) {
                            if (!list.contains(m)) list.add(m);
                            string = string.replace(part, "");
                        }
                    }
                } catch (InterruptedException | ExecutionException ignored) {
                    //Ignore
                }
            }
        }
        return new MentionList<>(string, list);
    }

    public static MentionList<Message> getMessagesId(Message message, String string) {
        ArrayList<Message> list = new ArrayList<>();

        for (String part : getArgs(string)) {
            for (ServerTextChannel channel : message.getServer().get().getTextChannels()) {
                Message m;
                try {
                    if (StringUtil.stringIsLong(part) && (m = channel.getMessageById(part).get()) != null) {
                        if (m.getChannel() == channel) {
                            if (!list.contains(m)) list.add(m);
                            string = string.replace(part, "");
                        }
                    }
                } catch (InterruptedException | ExecutionException ignored) {
                    //Ignore
                }
            }
        }
        return new MentionList<>(string, list);
    }

    public static MentionList<Message> getMessagesId(Message message, String string, ServerTextChannel channel) {
        ArrayList<Message> list = new ArrayList<>();

        if (!message.getServer().get().getTextChannels().contains(channel)) new MentionList<>(string, list);

        for (String part : getArgs(string)) {
            Message m;
            try {
                if (StringUtil.stringIsLong(part) && (m = channel.getMessageById(part).get()) != null) {
                    if (m.getChannel() == channel) {
                        if (!list.contains(m)) list.add(m);
                        string = string.replace(part, "");
                    }
                }
            } catch (InterruptedException | ExecutionException ignored) {
                //Ignore
            }
        }
        return new MentionList<>(string, list);
    }

    public static MentionList<Message> getMessagesURL(Message message, String string) {
        ArrayList<Message> list = new ArrayList<>();
        for(String part: getArgs(string)) {
            String regex = String.format("https://.*discord.*.com/channels/%d/\\d*/\\d*", message.getServer().get().getId());
            if (Pattern.matches(regex, part)) {
                String[] parts = part.split("/");
                if (parts.length == 7) {
                    try {
                        Message m;
                        if (message.getServer().get().getTextChannelById(parts[5]).isPresent() && (m = message.getServer().get().getTextChannelById(parts[5]).get().getMessageById(parts[6]).get()) != null) {
                            if (m.getIdAsString().equals(parts[6])) {
                                if (!list.contains(m)) list.add(m);
                                string = string.replace(part, "");
                            }
                        }
                    } catch (InterruptedException | ExecutionException ignored) {
                        //Ignore
                    }
                }
            }
        }
        return new MentionList<>(string,list);
    }

    private static ArrayList<String> getArgs(String string) {
        ArrayList<String> list = new ArrayList<>();
        if (string.length() > 0) {
            list.add(string);
            if (string.contains(" ")) {
                for (String part : string.split(" ")) {
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains("\n")) {
                for (String part : string.split("\n")) {
                    part = StringUtil.trimString(part);
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains("@")) {
                for (String part : string.split("@")) {
                    part = StringUtil.trimString(part);
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains(",")) {
                for (String part : string.split(",")) {
                    part = StringUtil.trimString(part);
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains("|")) {
                for (String part : string.split("\\|")) {
                    part = StringUtil.trimString(part);
                    if (part.length() > 0) list.add(part);
                }
            }
        }
        return list;
    }

    public static Mention getMentionedString(Locale locale, Message message, String followedString) {
        int counted = 0;
        boolean multi = false;
        Server server = message.getServer().get();

        MentionList<User> userMention = MentionUtil.getUsers(message, followedString);
        followedString = userMention.getResultMessageString();
        MentionList<Role> roleMention = MentionUtil.getRoles(message, followedString);
        followedString = roleMention.getResultMessageString();

        StringBuilder sb = new StringBuilder();

        for(User user: userMention.getList()) {
            sb.append("**").append(user.getDisplayName(server)).append("**, ");
            counted++;
        }

        for(Role role: roleMention.getList()) {
            sb.append("**").append(role.getName()).append("**, ");
            counted++;
            multi = true;
        }

        if (message.mentionsEveryone() || followedString.contains("everyone")) {
            if (counted == 0) sb.append("**").append(TextManager.getString(locale,TextManager.GENERAL,"everyone_start")).append("**, ");
            else sb.append("**").append(TextManager.getString(locale,TextManager.GENERAL,"everyone_end")).append("**, ");
            counted++;
            multi = true;
            followedString = followedString.replace("@everyone", "").replace("everyone", "");
        }

        if (counted == 0) return null;
        if (counted > 1) multi = true;

        String string = sb.toString();
        string = string.substring(0,string.length()-2);

        if (string.contains(", ")) string = StringUtil.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

        return new Mention(string, followedString, multi);
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

        if (string.contains(", ")) string = StringUtil.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

        return new Mention(string, null, multi);
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

        if (string.contains(", ")) string = StringUtil.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

        return new Mention(string, null, multi);
    }

    public static Optional<Role> getRoleByTag(Server server, String tag) {
        String id = tag.substring(3, tag.length() -1);
        return server.getRoleById(id);
    }

    public static ArrayList<KnownCustomEmoji> getCustomEmojiByTag(String string) {
        ArrayList<KnownCustomEmoji> knownCustomEmojis = new ArrayList<>();

        if (string.contains("<") && string.contains(">")) {
            for(String content: StringUtil.extractGroups(string, "<", ">")) {
                String[] tags = content.split(":");
                if (tags.length == 3) {
                    String id = tags[2];
                    DiscordApiCollection.getInstance().getCustomEmojiById(id).ifPresent(knownCustomEmojis::add);
                }
            }
        }

        return knownCustomEmojis;
    }

    public static long getAmountExt(String str, long available) {
        str = str.toLowerCase().replace("\n", " ");

        for(String part : str.split(" ")) {
            if (part.length() > 0) {
                if (part.equals("all") || part.equals("allin")) return available;
                if (part.equals("half")) return available / 2;

                double value = StringUtil.filterDoubleFromString(part);
                if (value != -1) {
                    if (part.endsWith("%")) return (long) Math.abs(value / 100.0 * available);

                    if (part.endsWith("k")) return (long) (value * 1000);
                    if (part.endsWith("m") || part.endsWith("mio") || part.endsWith("kk")) return (long) (value * 1000000);
                    if (part.endsWith("b") || part.endsWith("kkk")) return (long) (value * 1000000000);

                    return (long) value;
                }
            }
        }

        return -1;
    }

}
