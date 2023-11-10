package dashboard.components

import core.atomicassets.AtomicVoiceChannel
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener
import java.util.*

class DashboardVoiceChannelComboBox(label: String, locale: Locale, guildId: Long, selectedChannel: Long?, canBeEmpty: Boolean,
                                    action: DashboardEventListener<String>
) : DashboardComboBox(label, DataType.VOICE_CHANNELS, canBeEmpty, 1) {

    init {
        selectedValues = selectedChannel?.let {
            val atomicChannel = AtomicVoiceChannel(guildId, it)
            listOf(DiscordEntity(it.toString(), atomicChannel.getPrefixedName(locale)))
        } ?: emptyList<DiscordEntity>()
        setActionListener(action)
    }

}