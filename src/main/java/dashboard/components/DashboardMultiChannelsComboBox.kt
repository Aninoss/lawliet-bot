package dashboard.components

import commands.Command
import commands.CommandManager
import core.MemberCacheController
import core.ShardManager
import core.atomicassets.AtomicGuildChannel
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import kotlin.reflect.KClass

class DashboardMultiChannelsComboBox(
        dashboardCategory: DashboardCategory,
        label: String,
        dataType: DataType = DataType.GUILD_MESSAGE_CHANNELS,
        selectedChannelsSupplier: (GuildEntity) -> MutableList<Long>,
        canBeEmpty: Boolean,
        max: Int,
        commandAccessRequirement: KClass<out Command>? = null,
        botLogEvent: BotLogEntity.Event? = null
) : DashboardComboBox(label, dataType, canBeEmpty, max) {

    init {
        val guildId = dashboardCategory.atomicGuild.idLong
        val memberId = dashboardCategory.atomicMember.idLong

        selectedValues = selectedChannelsSupplier(dashboardCategory.guildEntity).map {
            val atomicChannel = AtomicGuildChannel(guildId, it)
            DiscordEntity(it.toString(), atomicChannel.getPrefixedName(dashboardCategory.locale))
        }

        setActionListener {
            val guildEntity = dashboardCategory.guildEntity
            if (commandAccessRequirement != null) {
                val guild = ShardManager.getLocalGuildById(guildId).get()
                val member = MemberCacheController.getInstance().loadMember(guild, memberId).get()
                if (!CommandManager.commandIsEnabledEffectively(guildEntity, commandAccessRequirement.java, member, null)) {
                    return@setActionListener ActionResult()
                            .withRedraw()
                }
            }

            val selectedChannels = selectedChannelsSupplier(guildEntity)
            if (it.type == "add") {
                guildEntity.beginTransaction()
                selectedChannels += it.data.toLong()
                if (botLogEvent != null) {
                    BotLogEntity.log(dashboardCategory.entityManager, botLogEvent, guildId, memberId, it.data, null)
                }
                guildEntity.commitTransaction()
            } else if (it.type == "remove") {
                guildEntity.beginTransaction()
                selectedChannels -= it.data.toLong()
                if (botLogEvent != null) {
                    BotLogEntity.log(dashboardCategory.entityManager, botLogEvent, guildId, memberId, null, it.data)
                }
                guildEntity.commitTransaction()
            }

            ActionResult()
        }
    }

}