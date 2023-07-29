package dashboard.components

import commands.Command
import commands.CommandManager
import core.MemberCacheController
import core.ShardManager
import core.atomicassets.AtomicTextChannel
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import mysql.hibernate.entity.GuildEntity
import kotlin.reflect.KClass

class DashboardMultiTextChannelsComboBox(
        dashboardCategory: DashboardCategory,
        label: String,
        selectedChannelsSupplier: (GuildEntity) -> List<Long>,
        canBeEmpty: Boolean,
        max: Int,
        memberId: Long? = null,
        commandAccessRequirement: KClass<out Command>? = null
) : DashboardComboBox(label, DataType.TEXT_CHANNELS, canBeEmpty, max) {

    init {
        val guildId = dashboardCategory.atomicGuild.idLong

        selectedValues = selectedChannelsSupplier(dashboardCategory.guildEntity).map {
            val atomicChannel = AtomicTextChannel(guildId, it)
            DiscordEntity(it.toString(), atomicChannel.getPrefixedName(dashboardCategory.locale))
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
            var selectedChannels = selectedChannelsSupplier(guildEntity)

            if (it.type == "add") {
                guildEntity.beginTransaction()
                selectedChannels += it.data.toLong()
                guildEntity.commitTransaction()
            } else if (it.type == "remove") {
                guildEntity.beginTransaction()
                selectedChannels -= it.data.toLong()
                guildEntity.commitTransaction()
            }

            ActionResult()
        }
    }

}