package dashboard.components

import core.CustomObservableList
import core.atomicassets.AtomicTextChannel
import dashboard.ActionResult
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity

class DashboardMultipleTextChannelsComboBox(label: String, guildId: Long, val selectedChannels: CustomObservableList<Long>, canBeEmpty: Boolean, max: Int) :
    DashboardComboBox(label, DataType.TEXT_CHANNELS, canBeEmpty, max) {

    constructor(guildId: Long, selectedChannels: CustomObservableList<Long>, canBeEmpty: Boolean, max: Int) :
            this("", guildId, selectedChannels, canBeEmpty, max)

    init {
        selectedValues = selectedChannels.map {
            val atomicChannel = AtomicTextChannel(guildId, it)
            DiscordEntity(it.toString(), atomicChannel.prefixedName)
        }
        setActionListener {
            if (it.type == "add") {
                selectedChannels.add(it.data.toLong())
            } else if (it.type == "remove") {
                selectedChannels.remove(it.data.toLong())
            }
            ActionResult(false)
        }
    }

}