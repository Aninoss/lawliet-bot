package dashboard.components

import core.atomicassets.AtomicTextChannel
import dashboard.ActionResult
import dashboard.DashboardEvent
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity

class DashboardTextChannelComboBox(label: String, guildId: Long, val selectedChannel: Long?, canBeEmpty: Boolean, action: (DashboardEvent<String>) -> Any) :
    DashboardComboBox(label, DataType.TEXT_CHANNELS, canBeEmpty, 1) {

    constructor(guildId: Long, selectedChannel: Long, canBeEmpty: Boolean, action: (DashboardEvent<String>) -> Any) :
            this("", guildId, selectedChannel, canBeEmpty, action)

    init {
        selectedValues = selectedChannel?.let {
            val atomicChannel = AtomicTextChannel(guildId, it)
            listOf(DiscordEntity(it.toString(), atomicChannel.prefixedName))
        } ?: emptyList<DiscordEntity>()
        setActionListener {
            action.invoke(it)
            ActionResult()
        }
    }

}