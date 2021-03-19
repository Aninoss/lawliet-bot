package core.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import core.emoji.EmojiTable;
import core.MainLogger;
import core.ShardManager;
import core.TextManager;
import core.cache.PatternCache;
import core.mention.Mention;
import core.mention.MentionList;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

public class MentionUtil {

    public static MentionList<Member> getMembers(Message message, String input) {
        return getMembers(message, input, message.getGuild().getMembers());
    }

    public static MentionList<Member> getMembers(Message message, String input, List<Member> members) {
        ArrayList<Member> list = new ArrayList<>(message.getMentionedMembers());
        if (!input.contains(ShardManager.getInstance().getSelfIdString())) {
            list.remove(message.getGuild().getSelfMember());
        }
        list.removeIf(member -> !members.contains(member));

        for (Member member : list) {
            input = input
                    .replace(getUserAsMention(member.getIdLong(), true), "")
                    .replace(getUserAsMention(member.getIdLong(), false), "");
        }

        return generateMentionList(
                members,
                list,
                input,
                u -> ((Member) u).getId(),
                u -> "@" + ((Member) u).getUser().getAsTag(),
                u -> "@" + ((Member) u).getUser().getName(),
                u -> "@" + ((Member) u).getEffectiveName() + "#" + ((Member) u).getUser().getDiscriminator(),
                u -> "@" + ((Member) u).getEffectiveName(),
                u -> ((Member) u).getUser().getAsTag(),
                u -> ((Member) u).getUser().getName(),
                u -> ((Member) u).getEffectiveName()
        );
    }

    public static MentionList<User> getUsers(Message message, String input) {
        return getUsers(message, input, message.getGuild().getMembers().stream().map(Member::getUser).collect(Collectors.toList()));
    }

    public static MentionList<User> getUsers(Message message, String input, List<User> users) {
        ArrayList<User> list = message.getMentionedMembers().stream().map(Member::getUser).collect(Collectors.toCollection(ArrayList::new));
        if (!input.contains(ShardManager.getInstance().getSelfIdString())) {
            list.remove(message.getGuild().getSelfMember().getUser());
        }
        list.removeIf(user -> !users.contains(user));

        for (User user : list) {
            input = input
                    .replace(getUserAsMention(user.getIdLong(), false), "")
                    .replace(getUserAsMention(user.getIdLong(), true), "");
        }

        return generateMentionList(
                users,
                list,
                input,
                u -> ((User) u).getId(),
                u -> "@" + ((User) u).getAsTag(),
                u -> "@" + ((User) u).getName(),
                u -> ((User) u).getAsTag(),
                u -> ((User) u).getName()
        );
    }

    public static CompletableFuture<MentionList<User>> getUsersFromString(String input, boolean onlyOne) {
        return CompletableFuture.supplyAsync(() -> {
            String newInput = input;
            ArrayList<User> userList = new ArrayList<>();
            ArrayList<Long> usedIds = new ArrayList<>();

            for (String segment : input.split(" ")) {
                String idString = segment;

                Matcher matcher = Message.MentionType.USER.getPattern().matcher(segment);
                if (matcher.matches()) {
                    idString = matcher.group(1);
                }

                if (StringUtil.stringIsLong(idString)) {
                    long userId = Long.parseUnsignedLong(idString);
                    if (!usedIds.contains(userId)) {
                        usedIds.add(userId);
                        if (NumberUtil.countDigits(userId) >= 17) {
                            try {
                                User user = ShardManager.getInstance().fetchUserById(userId).get();
                                if (!userList.contains(user)) {
                                    userList.add(user);
                                    newInput = newInput.replace(segment, "");
                                    if (onlyOne) {
                                        break;
                                    }
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                //Ignore
                            }
                        }

                        if (usedIds.size() >= 10) {
                            break;
                        }
                    }
                }
            }

            return new MentionList<>(newInput, userList);
        });
    }

    public static MentionList<Role> getRoles(Message message, String input) {
        ArrayList<Role> list = new ArrayList<>(message.getMentionedRoles());
        list.removeIf(role -> !message.getGuild().getRoles().contains(role));

        for (Role role : list) {
            input = input.replace(role.getAsMention(), "");
        }

        return generateMentionList(
                message.getGuild().getRoles(),
                list,
                input,
                r -> ((Role) r).getId(),
                r -> "@" + ((Role) r).getName(),
                r -> ((Role) r).getName()
        );
    }

    public static MentionList<TextChannel> getTextChannels(Message message, String input) {
        ArrayList<TextChannel> list = new ArrayList<>(message.getMentionedChannels());
        list.removeIf(channel -> !message.getGuild().getTextChannels().contains(channel));

        for (TextChannel channel : list) {
            input = input.replace(channel.getAsMention(), "");
        }

        return generateMentionList(
                message.getGuild().getTextChannels(),
                list,
                input,
                c -> ((TextChannel) c).getId(),
                c -> "#" + ((TextChannel) c).getName(),
                c -> ((TextChannel) c).getName()
        );
    }

    public static MentionList<VoiceChannel> getVoiceChannels(Message message, String input) {
        ArrayList<VoiceChannel> list = new ArrayList<>();

        return generateMentionList(
                message.getGuild().getVoiceChannels(),
                list,
                input,
                c -> ((VoiceChannel) c).getId(),
                c -> "#" + ((VoiceChannel) c).getName(),
                c -> ((VoiceChannel) c).getName()
        );
    }

    private static <T> MentionList<T> generateMentionList(Collection<T> sourceList, ArrayList<T> mentionList, String input, MentionFunction... functions) {
        if (mentionList.size() > 0) {
            return new MentionList<>(input, mentionList);
        }

        for (MentionFunction function : functions) {
            boolean found = false;

            for (T t : sourceList) {
                String tag = function.apply(t);
                if (matches(input, tag)) {
                    input = input.replaceAll("(?i)" + Pattern.quote(tag), "");
                    if (!mentionList.contains(t)) {
                        mentionList.add(t);
                    }
                    found = true;
                }
            }

            if (found) {
                break;
            }
        }

        input = input.trim();
        return new MentionList<>(input, mentionList);
    }

    private static boolean matches(String str, String check) {
        check = check.toLowerCase();
        str = " " + str.toLowerCase().replace("\n", " ") + " ";
        return str.contains(" " + check + " ");
    }

    public static MentionList<URL> getImages(String string) {
        ArrayList<URL> list = new ArrayList<>();

        for (String part : getUrlArgs(string)) {
            if (urlContainsImage(part)) {
                if (!part.contains(" ") && !part.contains("\n")) {
                    try {
                        URL urlTemp = new URL(part);
                        if (!list.contains(urlTemp)) list.add(urlTemp);
                        string = string.replace(part, "");
                    } catch (MalformedURLException e) {
                        MainLogger.get().error("Wrong url", e);
                    }
                }
            }
        }

        return new MentionList<>(string, list);
    }

    private static ArrayList<String> getUrlArgs(String string) {
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
                    part = part.trim();
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains("@")) {
                for (String part : string.split("@")) {
                    part = part.trim();
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains(",")) {
                for (String part : string.split(",")) {
                    part = part.trim();
                    if (part.length() > 0) list.add(part);
                }
            }
            if (string.contains("|")) {
                for (String part : string.split("\\|")) {
                    part = part.trim();
                    if (part.length() > 0) list.add(part);
                }
            }
        }
        return list;
    }

    private static boolean urlContainsImage(String url) {
        String fileType;
        try {
            URLConnection conn = new URL(url).openConnection();
            if (conn == null) return false;
            fileType = conn.getContentType().toLowerCase();

            for (int i = 0; i < 2; i++) {
                if (fileType.endsWith("jpg") || fileType.endsWith("jpeg") || fileType.endsWith("png") || fileType.endsWith("bmp") || fileType.endsWith("webp")) {
                    return true;
                }
                fileType = url.toLowerCase();
            }

            return false;
        } catch (IOException e) {
            //Ignore
        }
        return false;
    }

    public static CompletableFuture<MentionList<Message>> getMessageWithLinks(Message message, String link) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Message> list = new ArrayList<>();
            Guild guild = message.getGuild();
            String guildId = guild.getId();
            Matcher m = Message.JUMP_URL_PATTERN.matcher(link);
            while (m.find()) {
                String groupString = m.group("guild");
                if (groupString != null && groupString.equals(guildId)) {
                    Optional.ofNullable(guild.getTextChannelById(m.group("channel"))).ifPresent(channel -> {
                        try {
                            if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_HISTORY)) {
                                list.add(channel.retrieveMessageById(m.group("message")).complete());
                            }
                        } catch (Throwable e) {
                            //Ignore
                        }
                    });
                }
            }
            return new MentionList<>(link, list);
        });
    }

    public static MentionList<String> getEmojis(Message message, String input) {
        ArrayList<String> emojiList = new ArrayList<>();

        if (message != null) {
            for (Emote emote : message.getEmotes()) {
                emojiList.add(emote.getAsMention());
                input = input.replace(emote.getAsMention(), "");
            }
        }

        Optional<String> unicodeEmojiOpt = EmojiTable.getInstance().extractFirstEmoji(input);
        if (unicodeEmojiOpt.isPresent()) {
            String unicodeEmoji = unicodeEmojiOpt.get();
            emojiList.add(unicodeEmoji);
            input = input.replace(unicodeEmoji, "");
        }

        return new MentionList<>(input, emojiList);
    }

    private static Mention getMentionStringOfMentions(ArrayList<String> mentions, Locale locale, String filteredOriginalText, boolean multi, boolean containedBlockedUser) {
        if (mentions.size() > 1 && !multi) multi = true;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mentions.size(); i++) {
            if (i >= 1) {
                sb.append((i < mentions.size() - 1) ?
                        ", " :
                        " " + TextManager.getString(locale, TextManager.GENERAL, "and") + " "
                );
            }
            sb.append("**").append(mentions.get(i)).append("**");
        }

        return new Mention(sb.toString(), filteredOriginalText, multi, containedBlockedUser);
    }

    public static Mention getMentionedString(Locale locale, Message message, String args, Member blockedMember) {
        boolean multi = false;
        AtomicBoolean containedBlockedUser = new AtomicBoolean(false);
        final ArrayList<String> mentions = new ArrayList<>();

        /* add usernames */
        MentionList<Member> memberMention = MentionUtil.getMembers(message, args);
        memberMention.getList().forEach(member -> {
            if (blockedMember != null && member.getIdLong() == blockedMember.getIdLong()) {
                containedBlockedUser.set(true);
            } else {
                mentions.add(StringUtil.escapeMarkdown(member.getEffectiveName()));
            }
        });
        args = memberMention.getFilteredArgs();

        /* add role names */
        MentionList<Role> roleMention = MentionUtil.getRoles(message, args);
        roleMention.getList().forEach(role -> mentions.add(StringUtil.escapeMarkdown(role.getName())));
        args = roleMention.getFilteredArgs();

        /* add everyone mention */
        if (message.mentionsEveryone() || args.contains("everyone") || args.contains("all") || args.contains("@here")) {
            if (mentions.isEmpty()) {
                mentions.add(TextManager.getString(locale, TextManager.GENERAL, "everyone_start"));
            } else {
                mentions.add(TextManager.getString(locale, TextManager.GENERAL, "everyone_end"));
            }

            multi = true;
            args = args.replace("@everyone", "")
                    .replace("everyone", "")
                    .replace("all", "")
                    .replace("@here", "");
        }

        return getMentionStringOfMentions(mentions, locale, args, multi, containedBlockedUser.get());
    }

    public static Mention getMentionedStringOfMembers(Locale locale, List<Member> memberList) {
        final ArrayList<String> mentions = new ArrayList<>();

        /* add usernames */
        memberList.forEach(member -> mentions.add(StringUtil.escapeMarkdown(member.getEffectiveName())));

        return getMentionStringOfMentions(mentions, locale, null, false, false);
    }

    public static Mention getMentionedStringOfDiscriminatedMembers(Locale locale, List<Member> memberList) {
        return getMentionedStringOfDiscriminatedUsers(
                locale,
                memberList.stream().map(Member::getUser).collect(Collectors.toList())
        );
    }

    public static Mention getMentionedStringOfDiscriminatedUsers(Locale locale, List<User> userList) {
        final ArrayList<String> mentions = new ArrayList<>();

        /* add usernames */
        userList.forEach(user -> mentions.add(StringUtil.escapeMarkdown(user.getAsTag())));

        return getMentionStringOfMentions(mentions, locale, null, false, false);
    }

    public static Mention getMentionedStringOfRoles(Locale locale, List<Role> roleList) {
        final ArrayList<String> mentions = new ArrayList<>();

        /* add usernames */
        roleList.forEach(role -> mentions.add(StringUtil.escapeMarkdown(role.getName())));

        return getMentionStringOfMentions(mentions, locale, null, false, false);
    }

    public static Optional<Role> getRoleByTag(Guild guild, String tag) {
        String id = tag.substring(3, tag.length() - 1);
        return Optional.ofNullable(guild.getRoleById(id));
    }

    public static long getAmountExt(String str) {
        return getAmountExt(str, -1);
    }

    public static long getAmountExt(String str, long available) {
        str = reformatForDigits(str);

        for (String part : str.split(" ")) {
            if (part.length() > 0) {
                if (available >= 0 && (part.equals("all") || part.equals("allin"))) {
                    return available;
                }
                if (available >= 0 && part.equals("half")) {
                    return available / 2;
                }

                String valueString = StringUtil.filterDoubleString(part);
                String partPostfix = part.substring(valueString.length()).toLowerCase();
                if (valueString.isEmpty()) {
                    continue;
                }

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
        long min = 0;
        str = reformatForDigits(str);

        for (String part : str.split(" ")) {
            if (part.length() > 0) {
                long value = StringUtil.filterLongFromString(part);
                if (value > 0) {
                    String partPostfix = part.substring(String.valueOf(value).length()).toLowerCase();

                    switch (partPostfix) {
                        case "m":
                        case "min":
                            min += value;
                            break;

                        case "h":
                            min += value * 60;
                            break;

                        case "d":
                            min += value * 60 * 24;
                            break;

                        default:
                    }
                }
            }
        }

        return min;
    }

    private static String reformatForDigits(String str) {
        str = " " + str.toLowerCase()
                .replace("\n", " ")
                .replaceAll(" {2}", " ");

        Pattern p = PatternCache.getInstance().generate(" [0-9]+ [0-9]");
        Matcher m = p.matcher(str);
        while (m.find()) {
            String group = m.group();
            String groupNew = StringUtil.replaceLast(group, " ", "");
            str = str.replace(group, groupNew);
            m = p.matcher(str);
        }

        return str;
    }

    public static String getUserAsMention(long id, boolean withExclamationMark) {
        if (withExclamationMark) {
            return "<@!" + id + ">";
        } else {
            return "<@" + id + ">";
        }
    }


    private interface MentionFunction extends Function<Object, String> {

    }

}
