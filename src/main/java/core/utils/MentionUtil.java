package core.utils;

import com.vdurmont.emoji.EmojiParser;
import core.DiscordApiCollection;
import core.TextManager;
import core.mention.Mention;
import core.mention.MentionList;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(MentionUtil.class);
    private final static Pattern p = Pattern.compile(" [0-9]+ [0-9]");

    public static MentionList<User> getUsers(Message message, String content) {
        return getUsers(message, content, message.getServer().get().getMembers());
    }

    public static MentionList<User> getUsers(Message message, String content, Collection<User> users) {
        ArrayList<User> list = new ArrayList<>(message.getMentionedUsers());
        if (!content.contains(DiscordApiCollection.getInstance().getYourself().getIdAsString()))
            list.remove(DiscordApiCollection.getInstance().getYourself());
        list.removeIf(user -> !users.contains(user));

        for (User user : list)
            content = content
                    .replace(user.getMentionTag(), "")
                    .replace("<@!" + user.getIdAsString() + ">", "");

        for(User user : users) {
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
                content = content.replaceAll("(?i)" + Pattern.quote(name), "");
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

    public static MentionList<Emoji> getEmojis(Message message, String content) {
        ArrayList<Emoji> emojiList = new ArrayList<>();

        if (message != null) {
            for (CustomEmoji customEmoji : message.getCustomEmojis()) {
                emojiList.add(customEmoji);
                content = content.replace(customEmoji.getMentionTag(), "");
            }
        }

        List<String> unicodeEmojis = EmojiParser.extractEmojis(content);
        for (String unicodeEmoji : unicodeEmojis) {
            emojiList.add(StringUtil.unicodeToEmoji(unicodeEmoji));
            content = content.replace(unicodeEmoji, "");
        }

        return new MentionList<>(content, emojiList);
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

    private static Mention getMentionStringOfMentions(ArrayList<String> mentions, Locale locale, String filteredOriginalText, boolean multi, boolean containedBlockedUser) {
        if (mentions.size() > 1 && !multi) multi = true;

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < mentions.size(); i++) {
            if (i >= 1) {
                sb.append((i < mentions.size() - 1) ?
                        ", " :
                        " " + TextManager.getString(locale,TextManager.GENERAL,"and") + " "
                );
            }
            sb.append("**").append(mentions.get(i)).append("**");
        }

        return new Mention(sb.toString(), filteredOriginalText, multi, containedBlockedUser);
    }

    public static Mention getMentionedString(Locale locale, Message message, String followedString, User blockedUser) {
        Server server = message.getServer().get();
        boolean multi = false;
        AtomicBoolean containedBlockedUser = new AtomicBoolean(false);
        final ArrayList<String> mentions = new ArrayList<>();

        /* add usernames */
        MentionList<User> userMention = MentionUtil.getUsers(message, followedString);
        userMention.getList().forEach(user -> {
            if (blockedUser != null && user.getId() == blockedUser.getId()) {
                containedBlockedUser.set(true);
            } else {
                mentions.add(StringUtil.escapeMarkdown(user.getDisplayName(server)));
            }
        });
        followedString = userMention.getResultMessageString();

        /* add role names */
        MentionList<Role> roleMention = MentionUtil.getRoles(message, followedString);
        roleMention.getList().forEach(role -> mentions.add(StringUtil.escapeMarkdown(role.getName())));
        followedString = roleMention.getResultMessageString();

        /* add everyone mention */
        if (message.mentionsEveryone() || followedString.contains("everyone") || followedString.contains("all") || followedString.contains("@here")) {
            if (mentions.isEmpty())
                mentions.add(TextManager.getString(locale,TextManager.GENERAL,"everyone_start"));
            else
                mentions.add(TextManager.getString(locale,TextManager.GENERAL,"everyone_end"));

            multi = true;
            followedString = followedString.replace("@everyone", "")
                    .replace("everyone", "")
                    .replace("all", "")
                    .replace("@here", "");
        }

        return getMentionStringOfMentions(mentions, locale, followedString, multi, containedBlockedUser.get());
    }

    public static Mention getMentionedStringOfUsers(Locale locale, Server server, List<User> userList) {
        boolean multi = false;
        final ArrayList<String> mentions = new ArrayList<>();

        /* add usernames */
        userList.forEach(user -> mentions.add(StringUtil.escapeMarkdown(user.getDisplayName(server))));

        return getMentionStringOfMentions(mentions, locale, null, multi, false);
    }

    public static Mention getMentionedStringOfDiscriminatedUsers(Locale locale, List<User> userList) throws IOException {
        boolean multi = false;
        final ArrayList<String> mentions = new ArrayList<>();

        /* add usernames */
        userList.forEach(user -> mentions.add(StringUtil.escapeMarkdown(user.getDiscriminatedName())));

        return getMentionStringOfMentions(mentions, locale, null, multi, false);
    }

    public static Mention getMentionedStringOfRoles(Locale locale, List<Role> roleList) {
        boolean multi = false;
        final ArrayList<String> mentions = new ArrayList<>();

        /* add usernames */
        roleList.forEach(role -> mentions.add(StringUtil.escapeMarkdown(role.getName())));

        return getMentionStringOfMentions(mentions, locale, null, multi, false);
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

    public static long getAmountExt(String str) {
        return getAmountExt(str, -1);
    }

    public static long getAmountExt(String str, long available) {
        str = reformatForDigits(str);

        for(String part : str.split(" ")) {
            if (part.length() > 0) {
                if (available >= 0 && (part.equals("all") || part.equals("allin")))
                    return available;
                if (available >= 0 && part.equals("half"))
                    return available / 2;

                String valueString = StringUtil.filterDoubleString(part);
                String partPostfix = part.substring(valueString.length()).toLowerCase();
                if (valueString.isEmpty())
                    continue;

                double value = Double.parseDouble(valueString);
                switch (partPostfix) {
                    case "":
                        return (long) value;

                    case "%":
                        if (available < 0) continue;
                        return (long) Math.abs(value / 100.0 * available);

                    case "k":
                        return (long) (value * 1000);

                    case "m":
                    case "mio":
                    case "kk":
                        return (long) (value * 1000000);

                    case "b":
                    case "kkk":
                        return (long) (value * 1000000000);

                    default:
                }
            }
        }

        return -1;
    }

    public static long getTimeMinutesExt(String str) {
        long sec = 0;
        str = reformatForDigits(str);

        for(String part : str.split(" ")) {
            if (part.length() > 0) {
                long value = StringUtil.filterLongFromString(part);
                if (value > 0) {
                    String partPostfix = part.substring(String.valueOf(value).length()).toLowerCase();

                    switch (partPostfix) {
                        case "m":
                        case "min":
                            sec += value;
                            break;

                        case "h":
                            sec += value * 60;
                            break;

                        case "d":
                            sec += value * 60 * 24;
                            break;

                        default:
                    }
                }
            }
        }

        return sec;
    }

    private static String reformatForDigits(String str) {
        str = " " + str.toLowerCase()
                .replace("\n", " ")
                .replaceAll(" {2}", " ")
                .replaceAll("[.+,]", "");

        Matcher m = p.matcher(str);
        while(m.find()) {
            String group = m.group();
            String groupNew = StringUtil.replaceLast(group, " ", "");
            str = str.replace(group, groupNew);
            m = p.matcher(str);
        }

        return str;
    }

}
