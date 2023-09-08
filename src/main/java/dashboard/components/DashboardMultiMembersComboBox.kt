package dashboard.components

import commands.Command
import commands.CommandManager
import core.MemberCacheController
import core.ShardManager
import core.atomicassets.AtomicMember
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import mysql.hibernate.entity.GuildEntity
import kotlin.reflect.KClass

class DashboardMultiMembersComboBox(
        dashboardCategory: DashboardCategory,
        label: String,
        selectedMembersSupplier: (GuildEntity) -> MutableList<Long>,
        canBeEmpty: Boolean,
        max: Int,
        memberId: Long? = null,
        commandAccessRequirement: KClass<out Command>? = null
) : DashboardComboBox(label, DataType.MEMBERS, canBeEmpty, max) {

    init {
        val guildId = dashboardCategory.atomicGuild.idLong

        selectedValues = selectedMembersSupplier(dashboardCategory.guildEntity).map {
            val atomicMember = AtomicMember(guildId, it)
            DiscordEntity(it.toString(), atomicMember.getTaggedName(dashboardCategory.locale))
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

            val guildEntity = dashboardCategory.guildEntity
            val selectedMembers = selectedMembersSupplier(guildEntity)

            if (it.type == "add") {
                guildEntity.beginTransaction()
                selectedMembers += it.data.toLong()
                guildEntity.commitTransaction()
            } else if (it.type == "remove") {
                guildEntity.beginTransaction()
                selectedMembers -= it.data.toLong()
                guildEntity.commitTransaction()
            }

            ActionResult()
        }
    }

}