package dashboard.components

import commands.Command
import commands.CommandManager
import core.CustomObservableList
import core.MemberCacheController
import core.ShardManager
import core.atomicassets.AtomicMember
import dashboard.ActionResult
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import kotlin.reflect.KClass

class DashboardMultiMembersComboBox(label: String, guildId: Long, val selectedMembers: CustomObservableList<Long>, canBeEmpty: Boolean, max: Int,
                                    memberId: Long? = null, commandAccessRequirement: KClass<out Command>? = null
) : DashboardComboBox(label, DataType.MEMBERS, canBeEmpty, max) {

    init {
        selectedValues = selectedMembers.map {
            val atomicMember = AtomicMember(guildId, it)
            DiscordEntity(it.toString(), atomicMember.taggedName)
        }
        setActionListener {
            if (commandAccessRequirement != null && memberId != null) {
                val guild = ShardManager.getLocalGuildById(guildId).get()
                val member = MemberCacheController.getInstance().loadMember(guild, memberId).get()
                if (!CommandManager.commandIsTurnedOnEffectively(commandAccessRequirement.java, member, null)) {
                    return@setActionListener ActionResult()
                        .withRedraw()
                }
            }

            if (it.type == "add") {
                selectedMembers.add(it.data.toLong())
            } else if (it.type == "remove") {
                selectedMembers.remove(it.data.toLong())
            }
            ActionResult()
        }
    }

}