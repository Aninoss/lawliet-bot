package dashboard.components

import core.atomicassets.AtomicGuildChannel
import dashboard.DashboardCategory
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener

class DashboardChannelComboBox(
        dashboardCategory: DashboardCategory,
        label: String,
        dataType: DataType = DataType.GUILD_MESSAGE_CHANNELS,
        selectedChannel: Long?,
        canBeEmpty: Boolean,
        action: DashboardEventListener<String>
) : DashboardComboBox(label, dataType, canBeEmpty, 1) {

    init {
        selectedValues = selectedChannel?.let {
            val atomicChannel = AtomicGuildChannel(dashboardCategory.atomicGuild.idLong, it)
            listOf(DiscordEntity(it.toString(), atomicChannel.getPrefixedName(dashboardCategory.locale)))
        } ?: emptyList<DiscordEntity>()
        setActionListener(action)
    }

}