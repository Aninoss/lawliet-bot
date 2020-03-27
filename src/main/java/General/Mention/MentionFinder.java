package General.Mention;

import General.DiscordApiCollection;
import General.Tools;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class MentionFinder {

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
        for(User user: new ArrayList<>(list)) {
            if (!members.contains(user)) list.remove(user);
        }

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
                        e.printStackTrace();
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
        for(Role role: new ArrayList<>(list)) {
            if (!roles.contains(role)) list.remove(role);
        }

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
        for(ServerTextChannel channel: new ArrayList<>(list)) {
            if (!channels.contains(channel)) list.remove(channel);
        }

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
        for(ServerVoiceChannel channel: new ArrayList<>(list)) {
            if (!channels.contains(channel)) list.remove(channel);
        }

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
                    if (Tools.stringIsLong(part) && (m = channel.getMessageById(part).get()) != null) {
                        if (m.getChannel() == channel) {
                            if (!list.contains(m)) list.add(m);
                            string = removeMentionFromString(string, part, "");
                        }
                    }
                } catch (InterruptedException | ExecutionException ignored) {
                    //Do nothing
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
                    if (Tools.stringIsLong(part) && (m = channel.getMessageById(part).get()) != null) {
                        if (m.getChannel() == channel) {
                            if (!list.contains(m)) list.add(m);
                            string = removeMentionFromString(string, part, "");
                        }
                    }
                } catch (InterruptedException | ExecutionException ignored) {
                    //Do nothing
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
                if (Tools.stringIsLong(part) && (m = channel.getMessageById(part).get()) != null) {
                    if (m.getChannel() == channel) {
                        if (!list.contains(m)) list.add(m);
                        string = removeMentionFromString(string, part, "");
                    }
                }
            } catch (InterruptedException | ExecutionException ignored) {
                //Do nothing
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
                        //Do nothing
                    }
                }
            }
        }
        return new MentionList<>(string,list);
    }

    public static MentionList<Message> getMessagesAll(Message message, String string, ServerTextChannel channel) {
        ArrayList<Message> list = new ArrayList<>();
        for(String part: getArgs(string)) {
            String prefix = "https://discordapp.com/channels/" + message.getServer().get().getId() + "/" + channel + "/";
            if (part.startsWith(prefix)) {
                part = part.substring(prefix.length());
                Message m;
                try {
                    if ((m = channel.getMessageById(part).get()) != null) {
                        if (m.getIdAsString().equals(part)) {
                            if (!list.contains(m)) list.add(m);
                            string = removeMentionFromString(string, part, "");
                        }
                    }
                } catch (InterruptedException | ExecutionException ignored) {
                    //Do nothing
                }
            }

            Message m;
            try {
                if (Tools.stringIsLong(part) && (m = channel.getMessageById(part).get()) != null) {
                    if (m.getChannel() == channel) {
                        if (!list.contains(m)) list.add(m);
                        string = removeMentionFromString(string, part, "");
                    }
                }
            } catch (InterruptedException | ExecutionException ignored) {
                //Do nothing
            }
        }
        return new MentionList<>(string,list);
    }

    public static Message getMessageSearch(String searchTerm, TextChannel channel, Message messageStarter) throws ExecutionException, InterruptedException {
        if (!channel.canYouReadMessageHistory()) return null;

        searchTerm = Tools.cutSpaces(searchTerm);
        for(Message message: channel.getMessagesBefore(100,messageStarter).get().descendingSet()) {
            if (message.getContent().toLowerCase().contains(searchTerm.toLowerCase())) return message;
        }
        return null;
    }

    public static Message getMessageSearch(String searchTerm, Message messageStarter) throws ExecutionException, InterruptedException {
        for(ServerTextChannel channel: messageStarter.getServer().get().getTextChannels()) {
            Message message = getMessageSearch(searchTerm, channel, messageStarter);
            if (message != null) return message;
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
                    part = Tools.cutSpaces(part);
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains("@")) {
                for (String part : string.split("@")) {
                    part = Tools.cutSpaces(part);
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains(",")) {
                for (String part : string.split(",")) {
                    part = Tools.cutSpaces(part);
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains("|")) {
                for (String part : string.split("\\|")) {
                    part = Tools.cutSpaces(part);
                    if (part.length() > 0) list.add(part);
                }
            }
        }
        return list;
    }


    private static String removeMentionFromString(String string, String mention, String prefix) {
        String str = string.replace(prefix+mention,"");
        return str.replace(mention,"");
    }
}
