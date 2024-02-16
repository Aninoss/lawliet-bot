package dashboard.components

import core.atomicassets.AtomicTextChannel
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener
import java.util.*

class DashboardTextChannelComboBox(
        label: String,
        locale: Locale,
        guildId: Long,
        selectedChannel: Long?,
        canBeEmpty: Boolean,
        action: DashboardEventListener<String>
) : DashboardComboBox(label, DataType.TEXT_CHANNELS, canBeEmpty, 1) {

    init {
        selectedValues = selectedChannel?.let {
            val atomicChannel = AtomicTextChannel(guildId, it)
            listOf(DiscordEntity(it.toString(), atomicChannel.getPrefixedName(locale)))
        } ?: emptyList<DiscordEntity>()
        setActionListener(action)
    }

}