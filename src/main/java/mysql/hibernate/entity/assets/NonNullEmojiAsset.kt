package mysql.hibernate.entity.assets

import net.dv8tion.jda.api.entities.emoji.Emoji

interface NonNullEmojiAsset {

    var emojiFormatted: String
    var emoji: Emoji
        get() = Emoji.fromFormatted(emojiFormatted)
        set(value) { emojiFormatted = value.formatted }

}