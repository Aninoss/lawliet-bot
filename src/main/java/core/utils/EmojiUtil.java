package core.utils;

import constants.Emojis;
import core.LocalFile;
import core.MainLogger;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class EmojiUtil {

    public static boolean equals(Emoji emoji, Emoji otherEmoji) {
        if (emoji == null || otherEmoji == null) {
            return false;
        }

        if (emoji instanceof CustomEmoji && otherEmoji instanceof CustomEmoji) {
            return ((CustomEmoji) emoji).getIdLong() == ((CustomEmoji) otherEmoji).getIdLong();
        } else {
            return emoji.getFormatted().equals(otherEmoji.getFormatted());
        }
    }

    public static UnicodeEmoji[] getMultipleFromUnicode(String[] unicode) {
        return Arrays.stream(unicode)
                .map(Emoji::fromUnicode)
                .toArray(UnicodeEmoji[]::new);
    }

    public static Optional<MessageReaction> getMessageReactionFromMessage(Message message, Emoji emoji) {
        return message.getReactions().stream()
                .filter(r -> EmojiUtil.equals(r.getEmoji(), emoji))
                .findFirst();
    }

    public static String getLoadingEmojiMention(GuildMessageChannel channel) {
        if (channel != null && BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_EXT_EMOJI)) {
            return Emojis.LOADING.getFormatted();
        } else {
            return "⏳";
        }
    }

    public static Emoji getEmojiFromOverride(Emoji def, String id) {
        String emojiOverride = getEmojiFromOverrideOrNull(id);
        return emojiOverride == null
                ? def
                : Emoji.fromFormatted(emojiOverride);
    }

    public static String getEmojiFromOverride(String def, String id) {
        return Objects.requireNonNullElse(getEmojiFromOverrideOrNull(id), def);
    }

    public static BufferedImage toImage(Emoji emoji) {
        try {
            if (emoji instanceof UnicodeEmoji) {
                String assetId = toTwemojiAssetId(emoji.getFormatted());
                LocalFile localFile = new LocalFile(LocalFile.Directory.CDN, "emoji_cache/" + assetId + ".png");
                if (!localFile.exists()) {
                    BufferedImage bufferedImage = ImageIO.read(new URL("https://twemoji.maxcdn.com/v/latest/72x72/" + assetId + ".png"));
                    if (bufferedImage != null) {
                        ImageIO.write(bufferedImage, "png", localFile);
                    } else {
                        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                        ImageIO.write(img, "png", localFile);
                    }
                }
                return ImageIO.read(localFile);
            } else if (emoji instanceof CustomEmoji) {
                LocalFile localFile = new LocalFile(LocalFile.Directory.CDN, "emoji_cache/" + ((CustomEmoji) emoji).getId() + ".png");
                if (!localFile.exists()) {
                    ((CustomEmoji) emoji).getImage().downloadToFile(localFile, 32).get();
                }
                return ImageIO.read(localFile);
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            MainLogger.get().error("Emoji cache error", e);
            return null;
        }
        return null;
    }

    private static String toTwemojiAssetId(String emoji) {
        return emoji.codePoints()
                .mapToObj(Integer::toHexString)
                .collect(Collectors.joining("-"))
                .toLowerCase();
    }

    private static String getEmojiFromOverrideOrNull(String id) {
        String emojiOverride = System.getenv("EMOJI_OVERRIDE_" + id);
        if (emojiOverride == null) {
            return null;
        }

        if (emojiOverride.startsWith("<")) {
            return emojiOverride;
        } else {
            return new String(Base64.getUrlDecoder().decode(emojiOverride));
        }
    }

}
