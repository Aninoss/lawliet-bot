package dashboard.components

import core.TextManager
import core.atomicassets.AtomicGuildChannel
import core.utils.BotPermissionUtil
import core.utils.JDAUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import java.util.*

class DashboardChannelComboBox(
        dashboardCategory: DashboardCategory,
        label: String,
        dataType: DataType = DataType.GUILD_MESSAGE_CHANNELS,
        selectedChannel: Long?,
        canBeEmpty: Boolean,
        checkPermissions: Array<Permission> = emptyArray(),
        checkPermissionsParentCategory: Array<Permission> = emptyArray(),
        action: DashboardEventListener<String>,
) : DashboardComboBox(label, dataType, canBeEmpty, 1) {

    init {
        selectedValues = selectedChannel?.let {
            val atomicChannel = AtomicGuildChannel(dashboardCategory.atomicGuild.idLong, it)
            listOf(DiscordEntity(it.toString(), atomicChannel.getPrefixedName(dashboardCategory.locale)))
        } ?: emptyList<DiscordEntity>()

        setActionListener {
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

            action.accept(it)
        }
    }

    private fun checkPermissions(locale: Locale, channel: GuildChannel, permissions: Array<Permission>): String? {
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