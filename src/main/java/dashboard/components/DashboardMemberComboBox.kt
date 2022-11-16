package dashboard.components

import core.atomicassets.AtomicMember
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener

class DashboardMemberComboBox(label: String, guildId: Long, selectedMember: Long?, canBeEmpty: Boolean, action: DashboardEventListener<String>
) : DashboardComboBox(label, DataType.MEMBERS, canBeEmpty, 1) {

    init {
        selectedValues = selectedMember?.let {
            val atomicMember = AtomicMember(guildId, it)
            listOf(DiscordEntity(it.toString(), atomicMember.taggedName))
        } ?: emptyList<DiscordEntity>()
        setActionListener(action)
    }

}