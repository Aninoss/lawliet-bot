package core.utils;

import constants.RegexPatterns;
import core.MainLogger;
import core.MemberCacheController;
import core.ShardManager;
import core.TextManager;
import core.emoji.EmojiTable;
import core.mention.Mention;
import core.mention.MentionList;
import core.mention.MentionValue;
import javafx.util.Pair;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

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

public class MentionUtil {

    public static MentionList<Member> getMembers(Guild guild, String input, Member memberInclude) {
        MemberCacheController.getInstance().loadMembersFull(guild).join();
        return getMembers(guild, input, guild.getMembers(), memberInclude);
    }

    public static MentionList<Member> getMembers(Guild guild, String input, List<Member> members, Member memberInclude) {
        MemberCacheController.getInstance().loadMembersFull(guild).join();
        return generateMentionList(
                members,
                memberInclude != null ? new ArrayList<>(List.of(memberInclude)) : new ArrayList<>(),
                input,
                u -> getUserAsMention(((Member) u).getIdLong(), true),
                u -> getUserAsMention(((Member) u).getIdLong(), false),
                u -> " " + ((Member) u).getId() + " ",
                u -> " @" + ((Member) u).getUser().getAsTag() + " ",
                u -> " @" + ((Member) u).getUser().getName() + " ",
                u -> " @" + ((Member) u).getEffectiveName() + "#" + ((Member) u).getUser().getDiscriminator() + " ",
                u -> " @" + ((Member) u).getEffectiveName() + " ",
                u -> " " + ((Member) u).getUser().getAsTag() + " ",
                u -> " " + ((Member) u).getUser().getName() + " ",
                u -> " " + ((Member) u).getEffectiveName() + " "
        );
    }

    public static MentionList<User> getUsers(Guild guild, String input, Member memberInclude) {
        MemberCacheController.getInstance().loadMembersFull(guild).join();
        return getUsers(input, guild.getMembers().stream().map(Member::getUser).collect(Collectors.toList()), memberInclude);
    }

    public static MentionList<User> getUsers(String input, List<User> users, Member memberInclude) {
        return generateMentionList(
                users,
                memberInclude != null ? new ArrayList<>(List.of(memberInclude.getUser())) : new ArrayList<>(),
                input,
                u -> getUserAsMention(((User) u).getIdLong(), true),
                u -> getUserAsMention(((User) u).getIdLong(), false),
                u -> " " + ((User) u).getId() + " ",
                u -> " @" + ((User) u).getAsTag() + " ",
                u -> " @" + ((User) u).getName() + " ",
                u -> " " + ((User) u).getAsTag() + " ",
                u -> " " + ((User) u).getName() + " "
        );
    }

    public static CompletableFuture<MentionList<User>> getUsersFromString(String input, boolean onlyOne) {
        return FutureUtil.supplyAsync(() -> {
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
                    long userId = Long.parseLong(idString);
                    if (!usedIds.contains(userId) && NumberUtil.countDigits(userId) >= 17) {
                        usedIds.add(userId);
                        try {
                            User user = ShardManager.fetchUserById(userId).get();
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
                        if (usedIds.size() >= 10) {
                            break;
                        }
                    }
                }
            }

            return new MentionList<>(newInput, userList);
        });
    }

    public static MentionList<Role> getRoles(Guild guild, String input) {
        return generateMentionList(
                guild.getRoles(),
                new ArrayList<>(),
                input,
                r -> ((Role) r).getAsMention(),
                r -> " " + ((Role) r).getId() + " ",
                r -> "@ " + ((Role) r).getName() + " ",
                r -> " " + ((Role) r).getName() + " "
        );
    }

    public static MentionList<TextChannel> getTextChannels(Guild guild, String input) {
        return generateMentionList(
                guild.getTextChannels(),
                new ArrayList<>(),
                input,
                c -> ((TextChannel) c).getAsMention(),
                c -> " " + ((TextChannel) c).getId() + " ",
                c -> " #" + ((TextChannel) c).getName() + " ",
                c -> " " + ((TextChannel) c).getName() + " "
        );
    }

    public static MentionList<StandardGuildMessageChannel> getStandardGuildMessageChannels(Guild guild, String input) {
        ArrayList<StandardGuildMessageChannel> channels = new ArrayList<>();
        channels.addAll(guild.getTextChannels());
        channels.addAll(guild.getNewsChannels());
        return generateMentionList(
                channels,
                new ArrayList<>(),
                input,
                c -> ((StandardGuildMessageChannel) c).getAsMention(),
                c -> " " + ((StandardGuildMessageChannel) c).getId() + " ",
                c -> " #" + ((StandardGuildMessageChannel) c).getName() + " ",
                c -> " " + ((StandardGuildMessageChannel) c).getName() + " "
        );
    }

    public static MentionList<GuildMessageChannel> getGuildMessageChannels(Guild guild, String input) {
        ArrayList<GuildMessageChannel> channels = new ArrayList<>();
        channels.addAll(guild.getTextChannels());
        channels.addAll(guild.getNewsChannels());
        channels.addAll(guild.getThreadChannels());
        return generateMentionList(
                channels,
                new ArrayList<>(),
                input,
                c -> ((GuildMessageChannel) c).getAsMention(),
                c -> " " + ((GuildMessageChannel) c).getId() + " ",
                c -> " #" + ((GuildMessageChannel) c).getName() + " ",
                c -> " " + ((GuildMessageChannel) c).getName() + " "
        );
    }

    public static MentionList<VoiceChannel> getVoiceChannels(Message message, String input) {
        return generateMentionList(
                message.getGuild().getVoiceChannels(),
                new ArrayList<>(),
                input,
                c -> ((VoiceChannel) c).getAsMention(),
                c -> " " + ((VoiceChannel) c).getId() + " ",
                c -> " #" + ((VoiceChannel) c).getName() + " ",
                c -> " " + ((VoiceChannel) c).getName() + " "
        );
    }

    private static <T> MentionList<T> generateMentionList(Collection<T> sourceList, ArrayList<T> mentionList,
                                                          String input, MentionFunction... functions
    ) {
        for (MentionFunction function : functions) {
            boolean found = false;
            for (T t : sourceList) {
                String tag = function.apply(t);
                if (tag.isBlank()) {
                    continue;
                }

                if (!StringUtil.stringIsInt(tag.trim()) && matches(input, tag)) {
                    input = input.replaceAll("(?i)" + Pattern.quote(tag.trim()), "");
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

    private static boolean matches(String input, String check) {
        input = " " + input.toLowerCase().replace("\n", " ") + " ";
        return input.contains(check.toLowerCase());
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

    public static CompletableFuture<MentionList<Message>> getMessageWithLinks(Guild guild, String link) {
        return FutureUtil.supplyAsync(() -> {
            ArrayList<Message> list = new ArrayList<>();
            String guildId = guild.getId();
            Matcher m = Message.JUMP_URL_PATTERN.matcher(link);
            while (m.find()) {
                String groupString = m.group("guild");
                if (groupString != null && groupString.equals(guildId)) {
                    Optional.ofNullable(guild.getChannelById(StandardGuildMessageChannel.class, m.group("channel"))).ifPresent(channel -> {
                        try {
                            if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_HISTORY)) {
                                Message message = channel.retrieveMessageById(m.group("message")).complete();
                                if (JDAUtil.messageIsUserGenerated(message)) {
                                    list.add(message);
                                }
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

    public static MentionList<Emoji> getEmojis(Message message, String input) {
        ArrayList<Emoji> emojiList = new ArrayList<>();

        if (message != null) {
            for (CustomEmoji customEmoji : message.getMentions().getCustomEmojis()) {
                emojiList.add(customEmoji);
                input = input.replace(customEmoji.getFormatted(), "");
            }
        }

        Optional<UnicodeEmoji> unicodeEmojiOpt = EmojiTable.extractFirstUnicodeEmoji(input);
        if (unicodeEmojiOpt.isPresent()) {
            UnicodeEmoji unicodeEmoji = unicodeEmojiOpt.get();
            emojiList.add(unicodeEmoji);
            input = input.replace(unicodeEmoji.getFormatted(), "");
        }

        return new MentionList<>(input, emojiList);
    }

    public static MentionList<Emoji> getEmojis(Guild guild, String input) {
        MentionList<RichCustomEmoji> customEmojiMention = generateMentionList(
                guild.getEmojis(),
                new ArrayList<>(),
                input,
                e -> ((RichCustomEmoji) e).getFormatted(),
                e -> ((RichCustomEmoji) e).getAsReactionCode(),
                e -> " " + ((RichCustomEmoji) e).getId() + " ",
                e -> " :" + ((RichCustomEmoji) e).getName() + ": ",
                e -> " " + ((RichCustomEmoji) e).getName() + " "
        );

        List<Emoji> emojiList = new ArrayList<>(customEmojiMention.getList());
        input = customEmojiMention.getFilteredArgs();

        Optional<UnicodeEmoji> unicodeEmojiOpt = EmojiTable.extractFirstUnicodeEmoji(input);
        if (unicodeEmojiOpt.isPresent()) {
            UnicodeEmoji unicodeEmoji = unicodeEmojiOpt.get();
            emojiList.add(unicodeEmoji);
            input = input.replace(unicodeEmoji.getFormatted(), "");
        }

        for (String part : input.split(" ")) {
            if (StringUtil.stringIsLong(part)) {
                long emojiId = Long.parseLong(part);
                Optional<String> emojiStringOpt = ShardManager.getEmoteById(emojiId);
                if (emojiStringOpt.isPresent()) {
                    Emoji emoji = Emoji.fromFormatted(emojiStringOpt.get());
                    emojiList.add(emoji);
                    input = input.replace(part, "");
                }
            } else {
                try {
                    Emoji emoji = Emoji.fromFormatted(part);
                    if (emoji instanceof CustomEmoji) {
                        emojiList.add(emoji);
                        input = input.replace(part, "");
                    }
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }

        return new MentionList<>(input, emojiList);
    }

    private static Mention getMentionStringOfMentions(ArrayList<String> mentions, Locale locale, String filteredOriginalText,
                                                      boolean multi, boolean containedBlockedUser, List<ISnowflake> elementList
    ) {
        if (mentions.size() > 1 && !multi) multi = true;

        int size = Math.min(5, mentions.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i >= 1) {
                sb.append((i < size - 1) ?
                        ", " :
                        " " + TextManager.getString(locale, TextManager.GENERAL, "and") + " "
                );
            }
            sb.append("**");
            if (i < 4 || mentions.size() <= 5) {
                sb.append(mentions.get(i));
            } else {
                sb.append(TextManager.getString(locale, TextManager.GENERAL, "and_more", StringUtil.numToString(mentions.size() - 4)));
            }
            sb.append("**");
        }

        return new Mention(sb.toString(), filteredOriginalText, multi, containedBlockedUser, elementList);
    }

    public static Mention getMentionedString(Locale locale, Guild guild, String args, Member blockedMember, Member memberInclude) {
        boolean multi = false;
        AtomicBoolean containedBlockedUser = new AtomicBoolean(false);
        ArrayList<String> mentions = new ArrayList<>();
        ArrayList<ISnowflake> elementList = new ArrayList<>();

        /* add usernames */
        MentionList<Member> memberMention = MentionUtil.getMembers(guild, args, memberInclude);
        HashSet<Member> memberSet = new HashSet<>(memberMention.getList());
        memberSet.forEach(member -> {
            if (blockedMember != null && member.getIdLong() == blockedMember.getIdLong()) {
                containedBlockedUser.set(true);
            } else {
                elementList.add(member);
                mentions.add(StringUtil.escapeMarkdown(member.getEffectiveName()));
            }
        });
        args = memberMention.getFilteredArgs();

        /* add role names */
        MentionList<Role> roleMention = MentionUtil.getRoles(guild, args);
        roleMention.getList().forEach(role -> mentions.add(StringUtil.escapeMarkdown(role.getName())));
        args = roleMention.getFilteredArgs();

        /* add everyone mention */
        if (args.contains("everyone") || args.contains("@here")) {
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

        return getMentionStringOfMentions(mentions, locale, args, multi, containedBlockedUser.get(), elementList);
    }

    public static Mention getMentionedStringOfGuilds(Locale locale, List<Guild> guildList) {
        ArrayList<String> mentions = new ArrayList<>();
        ArrayList<ISnowflake> elementList = new ArrayList<>();
        guildList.forEach(guild -> {
            mentions.add(StringUtil.escapeMarkdown(guild.getName()));
            elementList.add(guild);
        });
        return getMentionStringOfMentions(mentions, locale, null, false, false, elementList);
    }

    public static Mention getMentionedStringOfMembers(Locale locale, List<Member> memberList) {
        ArrayList<String> mentions = new ArrayList<>();
        ArrayList<ISnowflake> elementList = new ArrayList<>();
        memberList.forEach(member -> {
            mentions.add(StringUtil.escapeMarkdown(member.getEffectiveName()));
            elementList.add(member);
        });
        return getMentionStringOfMentions(mentions, locale, null, false, false, elementList);
    }

    public static Mention getMentionedStringOfDiscriminatedUsers(Locale locale, List<User> userList) {
        ArrayList<String> mentions = new ArrayList<>();
        ArrayList<ISnowflake> elementList = new ArrayList<>();
        userList.forEach(user -> {
            mentions.add(StringUtil.escapeMarkdown(user.getAsTag()));
            elementList.add(user);
        });
        return getMentionStringOfMentions(mentions, locale, null, false, false, elementList);
    }

    public static Mention getMentionedStringOfRoles(Locale locale, List<Role> roleList) {
        ArrayList<String> mentions = new ArrayList<>();
        ArrayList<ISnowflake> elementList = new ArrayList<>();
        roleList.forEach(role -> {
            mentions.add(StringUtil.escapeMarkdown(role.getName()));
            elementList.add(role);
        });
        return getMentionStringOfMentions(mentions, locale, null, false, false, elementList);
    }

    public static Optional<Role> getRoleByTag(Guild guild, String tag) {
        String id = tag.substring(3, tag.length() - 1);
        return StringUtil.stringIsLong(id) ? Optional.ofNullable(guild.getRoleById(id)) : Optional.empty();
    }

    public static long getAmountExt(String str) {
        return getAmountExt(str, -1);
    }

    public static long getAmountExt(String str, long available) {
        str = str.toLowerCase();

        if (available >= 0) {
            if (str.contains("all") || str.contains("allin")) {
                return available;
            } else if (str.contains("half")) {
                return available / 2;
            }
        }

        str = reformatForDigits(str);
        Matcher m = RegexPatterns.AMOUNT_FILTER.matcher(str);
        while (m.find()) {
            double value = Double.parseDouble(m.group("digits").replace(",", "."));
            String unit = m.group("unit").toLowerCase();

            switch (unit) {
                case "%":
                    if (available < 0) continue;
                    return (long) Math.abs(value / 100.0 * available);

                case "k":
                case "thousand":
                case "thousands":
                    return (long) (value * 1_000.0);

                case "m":
                case "mio":
                case "million":
                case "millions":
                case "kk":
                    return (long) (value * 1_000_000.0);

                case "b":
                case "bio":
                case "billion":
                case "billions":
                case "kkk":
                    return (long) (value * 1_000_000_000.0);

                case "tri":
                case "trillion":
                case "trillions":
                case "kkkk":
                case "mm":
                    return (long) (value * 1_000_000_000_000.0);

                default:
                    return (long) value;
            }
        }

        return -1;
    }

    public static MentionValue<Long> getTimeMinutes(String str) {
        long min = 0;
        List<Pair<Pattern, Integer>> unitList = List.of(
                new Pair<>(RegexPatterns.MINUTES, 1),
                new Pair<>(RegexPatterns.HOURS, 60),
                new Pair<>(RegexPatterns.DAYS, 60 * 24)
        );

        for (Pair<Pattern, Integer> patternIntegerPair : unitList) {
            Matcher matcher = patternIntegerPair.getKey().matcher(str);
            while (matcher.find()) {
                String groupStr = matcher.group();
                min += StringUtil.filterLongFromString(groupStr) * patternIntegerPair.getValue();
                str = str.replace(groupStr, "");
            }
        }

        return new MentionValue<>(str, min);
    }

    public static String reformatForDigits(String str) {
        Pattern p = RegexPatterns.DIGIT_REFORMAT;
        Matcher m = p.matcher(str);
        while (m.find()) {
            String group = m.group();
            String groupNew = group.replaceAll("[\\s| ]s*", "");
            str = str.replace(group, groupNew);
            m = p.matcher(str);
        }

        return str.replaceAll("<[^>^]*>", "");
    }

    public static String getUserAsMention(long id, boolean withExclamationMark) {
        if (withExclamationMark) {
            return "<@!" + id + ">";
        } else {
            return "<@" + id + ">";
        }
    }

    public static HashSet<String> extractUserMentions(String content) {
        content = content.replace("<@!", "<@");
        String[] groups = StringUtil.extractGroups(content, "<@", ">");
        HashSet<String> set = new HashSet<>();
        for (String group : groups) {
            set.add("<@" + group + ">");
        }
        return set;
    }


    private interface MentionFunction extends Function<Object, String> {

    }

}
