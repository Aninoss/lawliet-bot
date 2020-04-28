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

    public static MentionList<User> getUsers(Message message, String string) {
        ArrayList<User> list = new ArrayList<>(message.getMentionedUsers());
        if (!string.contains(DiscordApiCollection.getInstance().getYourself().getIdAsString())) list.remove(DiscordApiCollection.getInstance().getYourself());
        for (User user : list) {
            string = string.replace(user.getMentionTag(), "").replace("<@!"+user.getIdAsString()+">", "");
        }

        for (String part : getArgs(string)) {
            if (part.startsWith("@")) part = part.substring(1);

            if (message.getServer().get().getMemberById(part).isPresent()) {
                User u = message.getServer().get().getMemberById(part).get();
                if (!list.contains(u)) list.add(u);
                string = removeMentionFromString(string, part, "@");
            }

            for (User u : message.getServer().get().getMembers()) {
                String disciminatedNickname = u.getDisplayName(message.getServer().get()) + "#" + u.getDiscriminator();

                if (u.getDiscriminatedName().equalsIgnoreCase(part) || disciminatedNickname.equalsIgnoreCase(part)) {
                    if (!list.contains(u)) list.add(u);
                    string = removeMentionFromString(string, part, "@");
                }
            }

            for (User u : message.getServer().get().getMembersByNameIgnoreCase(part)) {
                if (!list.contains(u)) list.add(u);
                string = removeMentionFromString(string, part, "@");
            }

            for (User u : message.getServer().get().getMembersByNicknameIgnoreCase(part)) {
                if (!list.contains(u)) list.add(u);
                string = removeMentionFromString(string, part, "@");
            }
        }

        Collection<User> members = message.getServer().get().getMembers();
        list.removeIf(user -> !members.contains(user));

        return new MentionList<>(string, list);
    }

    public static MentionList<URL> getImages(String string) {
        ArrayList<URL> list = new ArrayList<>();

        for (String part : getArgs(string)) {
            if (urlContainsImage(part)) {
                if (!part.contains(" ") && !part.contains("\n")) {
                    try {
                        URL urlTemp = new URL(part);
                        if (!list.contains(urlTemp)) list.add(urlTemp);
                        string = removeMentionFromString(string, part, "");
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

    public static MentionList<Role> getRoles(Message message, String string) {
        ArrayList<Role> list = new ArrayList<>(message.getMentionedRoles());
        for (Role role : list) {
            string = string.replace(role.getMentionTag(), "");
        }
        for (String part : getArgs(string)) {
            if (part.startsWith("@")) part = part.substring(1);

            if (message.getServer().get().getRoleById(part).isPresent()) {
                Role r = message.getServer().get().getRoleById(part).get();
                if (!list.contains(r)) list.add(r);
                string = removeMentionFromString(string, part, "@");
            }

            for (Role r : message.getServer().get().getRoles()) {
                if (r.getName().equalsIgnoreCase(part)) {
                    if (!list.contains(r)) list.add(r);
                    string = removeMentionFromString(string, part, "@");
                }
            }
        }

        Collection<Role> roles = message.getServer().get().getRoles();
        list.removeIf(role -> !roles.contains(role));

        return new MentionList<>(string, list);
    }

    public static MentionList<ServerTextChannel> getTextChannels(Message message, String string) {
        return getTextChannels(message, string, false);
    }

    public static MentionList<ServerTextChannel> getTextChannels(Message message, String string, boolean tagRequired) {
        ArrayList<ServerTextChannel> list = new ArrayList<>(message.getMentionedChannels());
        for (ServerTextChannel channel : list) {
            string = string.replace(channel.getMentionTag(), "");
        }
        for (String part : getArgs(string)) {
            if (part.startsWith("#")) part = part.substring(1);

            if (message.getServer().get().getTextChannelById(part).isPresent()) {
                ServerTextChannel r = message.getServer().get().getTextChannelById(part).get();
                if (!list.contains(r)) list.add(r);
                string = removeMentionFromString(string, part, "#");
            }

            if (!tagRequired) {
                for (ServerTextChannel sc : message.getServer().get().getTextChannelsByNameIgnoreCase(part)) {
                    if (!list.contains(sc)) list.add(sc);
                    string = removeMentionFromString(string, part, "#");
                }

                for (ChannelCategory c: message.getServer().get().getChannelCategories()) {
                    for (ServerChannel channel: c.getChannels()) {
                        if (channel.asServerTextChannel().isPresent() &&
                                channel.asServerTextChannel().get().getCategory().isPresent() &&
                                channel.asServerTextChannel().get().getCategory().get().getName().equalsIgnoreCase(part)
                        ) {
                            ServerTextChannel sc = channel.asServerTextChannel().get();
                            if (!list.contains(sc)) list.add(sc);
                            string = removeMentionFromString(string, part, "#");
                        }
                    }
                }
            }
        }

        Collection<ServerTextChannel> channels = message.getServer().get().getTextChannels();
        list.removeIf(channel -> !channels.contains(channel));

        return new MentionList<>(string, list);
    }

    public static MentionList<ServerVoiceChannel> getVoiceChannels(Message message, String string) {
        ArrayList<ServerVoiceChannel> list = new ArrayList<>();
        for (String part : getArgs(string)) {
            if (part.startsWith("#")) part = part.substring(1);

            if (message.getServer().get().getVoiceChannelById(part).isPresent()) {
                ServerVoiceChannel r = message.getServer().get().getVoiceChannelById(part).get();
                if (!list.contains(r)) list.add(r);
                string = removeMentionFromString(string, part, "#");
            }

            for (ServerVoiceChannel sc : message.getServer().get().getVoiceChannelsByNameIgnoreCase(part)) {
                if (!list.contains(sc)) list.add(sc);
                string = removeMentionFromString(string, part, "#");
            }

            for (ChannelCategory c: message.getServer().get().getChannelCategories()) {
                for (ServerChannel channel: c.getChannels()) {
                    if (channel.asServerVoiceChannel().isPresent() &&
                            channel.asServerVoiceChannel().get().getCategory().isPresent() &&
                            channel.asServerVoiceChannel().get().getCategory().get().getName().equalsIgnoreCase(part)
                        ) {
                        ServerVoiceChannel sc = channel.asServerVoiceChannel().get();
                        if (!list.contains(sc)) list.add(sc);
                        string = removeMentionFromString(string, part, "#");
                    }
                }
            }
        }

        Collection<ServerVoiceChannel> channels = message.getServer().get().getVoiceChannels();
        list.removeIf(channel -> !channels.contains(channel));

        return new MentionList<>(string, list);
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
                            string = removeMentionFromString(string, part, "");
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
                            string = removeMentionFromString(string, part, "");
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
                        string = removeMentionFromString(string, part, "");
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
            String regex = String.format("https://.*discordapp.com/channels/%d/\\d*/\\d*", message.getServer().get().getId());
            if (Pattern.matches(regex, part)) {
                String[] parts = part.split("/");
                if (parts.length == 7) {
                    try {
                        Message m;
                        if (message.getServer().get().getTextChannelById(parts[5]).isPresent() && (m = message.getServer().get().getTextChannelById(parts[5]).get().getMessageById(parts[6]).get()) != null) {
                            if (m.getIdAsString().equals(parts[6])) {
                                if (!list.contains(m)) list.add(m);
                                string = removeMentionFromString(string, part, "");
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

    public static Message getMessageSearch(String searchTerm, TextChannel channel, Message messageStarter) throws ExecutionException, InterruptedException {
        if (!channel.canYouReadMessageHistory()) return null;

        searchTerm = StringUtil.trimString(searchTerm);
        for(Message message: channel.getMessagesBefore(100,messageStarter).get().descendingSet()) {
            if (message.getContent().toLowerCase().contains(searchTerm.toLowerCase())) return message;
        }
        return null;
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
        List<User> userList = MentionUtil.getUsers(message, followedString).getList();
        List<Role> roleList = MentionUtil.getRoles(message, followedString).getList();
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

        if (string.contains(", ")) string = StringUtil.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

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

        if (string.contains(", ")) string = StringUtil.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

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

        if (string.contains(", ")) string = StringUtil.replaceLast(string,", "," "+TextManager.getString(locale,TextManager.GENERAL,"and")+" ");

        return new Mention(string, multi);
    }

    private static String removeMentionFromString(String string, String mention, String prefix) {
        String str = string.replace(prefix+mention,"");
        return str.replace(mention,"");
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
                if (part.equals("all")) return available;
                if (part.equals("half")) return available / 2;

                long value = StringUtil.filterNumberFromString(part);
                if (value != -1) {
                    if (part.endsWith("%")) return (long) Math.abs(value / 100.0 * available);

                    if (part.endsWith("k")) return value * 1000;
                    if (part.endsWith("m") || part.endsWith("mio")) return value * 1000000;
                    if (part.endsWith("b")) return value * 1000000000;

                    return value;
                }
            }
        }

        return -1;
    }

}
