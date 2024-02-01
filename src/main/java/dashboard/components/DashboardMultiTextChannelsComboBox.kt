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
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import kotlin.reflect.KClass

class DashboardMultiTextChannelsComboBox(
        dashboardCategory: DashboardCategory,
        label: String,
        selectedChannelsSupplier: (GuildEntity) -> MutableList<Long>,
        canBeEmpty: Boolean,
        max: Int,
        commandAccessRequirement: KClass<out Command>? = null,
        botLogEvent: BotLogEntity.Event? = null
) : DashboardComboBox(label, DataType.TEXT_CHANNELS, canBeEmpty, max) {

    init {
        val guildId = dashboardCategory.atomicGuild.idLong
        val memberId = dashboardCategory.atomicMember.idLong

        selectedValues = selectedChannelsSupplier(dashboardCategory.guildEntity).map {
            val atomicChannel = AtomicTextChannel(guildId, it)
            DiscordEntity(it.toString(), atomicChannel.getPrefixedName(dashboardCategory.locale))
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
            val selectedChannels = selectedChannelsSupplier(guildEntity)

            if (it.type == "add") {
                guildEntity.beginTransaction()
                selectedChannels += it.data.toLong()
                guildEntity.commitTransaction()
                if (botLogEvent != null) {
                    BotLogEntity.log(dashboardCategory.entityManager, botLogEvent, guildId, memberId, it.data, null)
                }
            } else if (it.type == "remove") {
                guildEntity.beginTransaction()
                selectedChannels -= it.data.toLong()
                guildEntity.commitTransaction()
                if (botLogEvent != null) {
                    BotLogEntity.log(dashboardCategory.entityManager, botLogEvent, guildId, memberId, null, it.data)
                }
            }

            ActionResult()
        }
    }

}