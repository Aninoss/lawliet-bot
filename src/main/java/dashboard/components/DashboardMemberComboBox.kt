package dashboard.components

import core.atomicassets.AtomicMember
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener
import java.util.*

class DashboardMemberComboBox(
        label: String,
        locale: Locale,
        guildId: Long,
        selectedMember: Long?,
        canBeEmpty: Boolean,
        action: DashboardEventListener<String>
) : DashboardComboBox(label, DataType.MEMBERS, canBeEmpty, 1) {

    init {
        selectedValues = selectedMember?.let {
            val atomicMember = AtomicMember(guildId, it)
            listOf(DiscordEntity(it.toString(), atomicMember.getTaggedName(locale)))
        } ?: emptyList<DiscordEntity>()
        setActionListener(action)
    }

}