package dashboard.components

import core.emoji.EmojiTable
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji

class DashboardEmojiComboBox(
        label: String,
        selectedEmoji: String?,
        canBeEmpty: Boolean,
        action: DashboardEventListener<String>
) : DashboardComboBox(label, DataType.EMOJI, canBeEmpty, 1) {

    init {
        selectedValues = selectedEmoji?.let {
            val emoji = Emoji.fromFormatted(it)
            if (emoji is CustomEmoji) {
                listOf(DiscordEntity(it, (emoji as CustomEmoji).name, emoji.imageUrl))
            } else {
                listOf(DiscordEntity(it, EmojiTable.getEmojiName(it), it))
            }
        } ?: emptyList<DiscordEntity>()
        setActionListener(action)
    }

}