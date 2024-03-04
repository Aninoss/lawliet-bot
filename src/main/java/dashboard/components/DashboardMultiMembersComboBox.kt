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
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import kotlin.reflect.KClass

class DashboardMultiMembersComboBox(
        dashboardCategory: DashboardCategory,
        label: String,
        selectedMembersSupplier: (GuildEntity) -> MutableList<Long>,
        canBeEmpty: Boolean,
        max: Int,
        commandAccessRequirement: KClass<out Command>? = null,
        botLogEvent: BotLogEntity.Event? = null
) : DashboardComboBox(label, DataType.MEMBERS, canBeEmpty, max) {

    init {
        val guildId = dashboardCategory.atomicGuild.idLong
        val memberId = dashboardCategory.atomicMember.idLong

        selectedValues = selectedMembersSupplier(dashboardCategory.guildEntity).map {
            val atomicMember = AtomicMember(guildId, it)
            DiscordEntity(it.toString(), atomicMember.getTaggedName(dashboardCategory.locale))
        }

        setActionListener {
            if (commandAccessRequirement != null) {
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
                if (botLogEvent != null) {
                    BotLogEntity.log(dashboardCategory.entityManager, botLogEvent, guildId, memberId, it.data, null)
                }
                guildEntity.commitTransaction()
            } else if (it.type == "remove") {
                guildEntity.beginTransaction()
                selectedMembers -= it.data.toLong()
                if (botLogEvent != null) {
                    BotLogEntity.log(dashboardCategory.entityManager, botLogEvent, guildId, memberId, null, it.data)
                }
                guildEntity.commitTransaction()
            }

            ActionResult()
        }
    }

}