package dashboard.components

import commands.Command
import commands.CommandManager
import core.MemberCacheController
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicGuildChannel
import core.utils.BotPermissionUtil
import core.utils.JDAUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import java.util.*
import kotlin.reflect.KClass

class DashboardMultiChannelsComboBox(
    dashboardCategory: DashboardCategory,
    label: String,
    dataType: DataType = DataType.GUILD_MESSAGE_CHANNELS,
    selectedChannelsSupplier: (GuildEntity) -> MutableList<Long>,
    canBeEmpty: Boolean,
    max: Int,
    commandAccessRequirement: KClass<out Command>? = null,
    botLogEvent: BotLogEntity.Event? = null,
    checkPermissions: Array<Permission> = emptyArray(),
    checkPermissionsParentCategory: Array<Permission> = emptyArray(),
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
                if (it.data != null) {
                    val channel = dashboardCategory.atomicGuild.get().get().getGuildChannelById(it.data)!!
                    val err = checkPermissions(dashboardCategory.locale, channel, checkPermissions)
                        ?: checkPermissions(dashboardCategory.locale, JDAUtil.getChannelParentCategory(channel), checkPermissionsParentCategory)
                    if (err != null) {
                        return@setActionListener ActionResult()
                            .withRedraw()
                            .withErrorMessage(err)
                    }
                }

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

    private fun checkPermissions(locale: Locale, channel: GuildChannel?, permissions: Array<Permission>): String? {
        if (channel == null) {
            return null
        }

        if (permissions.isNotEmpty()) {
            if (!BotPermissionUtil.can(channel, *permissions)) {
                val sb = StringBuilder()
                for (permission in permissions) {
                    if (sb.isNotEmpty()) {
                        sb.append(", ")
                    }
                    sb.append(TextManager.getString(locale, TextManager.PERMISSIONS, permission.name))
                }

                return TextManager.getString(locale, TextManager.COMMANDS, "stateprocessor_channels_missingpermissions", sb.toString(), AtomicGuildChannel(channel).getPrefixedName(locale));
            }
        }
        return null;
    }

}