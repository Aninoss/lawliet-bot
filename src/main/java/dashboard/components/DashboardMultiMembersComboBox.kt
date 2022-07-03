package dashboard.components

import core.CustomObservableList
import core.atomicassets.AtomicMember
import dashboard.ActionResult
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity

class DashboardMultiMembersComboBox(label: String, guildId: Long, val selectedMembers: CustomObservableList<Long>, canBeEmpty: Boolean, max: Int) :
    DashboardComboBox(label, DataType.MEMBERS, canBeEmpty, max) {

    constructor(guildId: Long, selectedMembers: CustomObservableList<Long>, canBeEmpty: Boolean, max: Int) :
            this("", guildId, selectedMembers, canBeEmpty, max)

    init {
        selectedValues = selectedMembers.map {
            val atomicMember = AtomicMember(guildId, it)
            DiscordEntity(it.toString(), atomicMember.taggedName)
        }
        setActionListener {
            if (it.type == "add") {
                selectedMembers.add(it.data.toLong())
            } else if (it.type == "remove") {
                selectedMembers.remove(it.data.toLong())
            }
            ActionResult()
        }
    }

}