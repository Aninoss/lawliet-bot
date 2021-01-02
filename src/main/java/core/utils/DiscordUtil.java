package core.utils;

import core.UnicodeEmoji;
import core.cache.PatternCache;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Icon;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.server.Server;
import org.javacord.api.util.DiscordRegexPattern;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;
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

    public static Optional<ServerTextChannel> getFirstWritableChannelOfServer(Server server) {
        if (server.getSystemChannel().isPresent() && server.getSystemChannel().get().canYouSee() && server.getSystemChannel().get().canYouWrite() && server.getSystemChannel().get().canYouEmbedLinks()) {
            return server.getSystemChannel();
        } else {
            for(ServerTextChannel channel : server.getTextChannels()) {
                if (channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks()) {
                    return Optional.of(channel);
                }
            }
        }

        return Optional.empty();
    }

    public static Emoji emojiFromString(String str) {
        if (str.startsWith("<"))
            return createCustomEmojiFromTag(str);
        else
            return UnicodeEmoji.fromString(str);
    }

    public static boolean emojiIsString(Emoji emoji, String emojiCompare) {
        return emojiIsEmoji(emoji, emojiFromString(emojiCompare));
    }

    public static boolean emojiIsEmoji(Emoji emoji, Emoji emoji2) {
        return emoji.asCustomEmoji().map(DiscordEntity::getIdAsString).orElse(emoji.getMentionTag())
                .equals(emoji2.asCustomEmoji().map(DiscordEntity::getIdAsString).orElse(emoji2.getMentionTag()));
    }

    public static CustomEmoji createCustomEmojiFromTag(String tag) {
        Matcher m = DiscordRegexPattern.CUSTOM_EMOJI.matcher(tag);
        if (m.find()) {
            String name = m.group("name");
            long id = Long.parseLong(m.group("id"));
            boolean animated = tag.startsWith("<a:");

            return new CustomEmoji() {
                @Override
                public Icon getImage() {
                    throw new NoSuchElementException("No image present");
                }

                @Override
                public DiscordApi getApi() {
                    throw new NoSuchElementException("No api present");
                }

                @Override
                public long getId() {
                    return id;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public boolean isAnimated() {
                    return animated;
                }
            };
        }

        throw new NoSuchElementException("Not a custom emoji tag");
    }

}
