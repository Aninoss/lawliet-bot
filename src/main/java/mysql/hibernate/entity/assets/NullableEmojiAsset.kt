package mysql.hibernate.entity.assets

import net.dv8tion.jda.api.entities.emoji.Emoji

interface NullableEmojiAsset {

    var emojiFormatted: String?
    var emoji: Emoji?
        get() = if (emojiFormatted != null) Emoji.fromFormatted(emojiFormatted!!) else null
        set(value) { emojiFormatted = value?.formatted }

}