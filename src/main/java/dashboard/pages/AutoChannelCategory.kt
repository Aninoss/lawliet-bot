package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.AutoChannelCommand
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardSeparator
import dashboard.component.DashboardSwitch
import dashboard.component.DashboardText
import dashboard.component.DashboardTextField
import dashboard.components.DashboardVoiceChannelComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.autochannel.DBAutoChannel
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "autochannel",
        userPermissions = [Permission.MANAGE_CHANNEL],
        botPermissions = [Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL],
        commandAccessRequirements = [AutoChannelCommand::class]
)
class AutoChannelCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(AutoChannelCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val activeSwitch = DashboardSwitch(getString(Category.UTILITY, "autochannel_state0_mactive")) {
            if (it.data != DBAutoChannel.getInstance().retrieve(guild.idLong).isActive) {
                DBAutoChannel.getInstance().retrieve(guild.idLong).toggleActive()
            }
            return@DashboardSwitch ActionResult()
        }
        activeSwitch.isChecked = DBAutoChannel.getInstance().retrieve(guild.idLong).isActive
        mainContainer.add(activeSwitch, DashboardSeparator())

        val channelComboBox = DashboardVoiceChannelComboBox(
                getString(Category.UTILITY, "autochannel_state0_mchannel"),
                locale,
                atomicGuild.idLong,
                DBAutoChannel.getInstance().retrieve(guild.idLong).parentChannelId.orElse(null),
                false,
        ) {
            val channel = atomicGuild.get().get().getVoiceChannelById(it.data)
            if (channel == null) {
                return@DashboardVoiceChannelComboBox ActionResult()
                        .withRedraw()
            }

            val channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(locale, channel, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)
            if (channelMissingPerms != null) {
                return@DashboardVoiceChannelComboBox ActionResult()
                        .withErrorMessage(channelMissingPerms)
                        .withRedraw()
            }

            val parent = channel.getParentCategory()
            if (parent != null) {
                val categoryMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(locale, parent, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)
                if (categoryMissingPerms != null) {
                    return@DashboardVoiceChannelComboBox ActionResult()
                            .withErrorMessage(categoryMissingPerms)
                            .withRedraw()
                }
            }

            DBAutoChannel.getInstance().retrieve(guild.idLong).setParentChannelId(it.data.toLong())
            return@DashboardVoiceChannelComboBox ActionResult()
        }
        mainContainer.add(channelComboBox, DashboardSeparator())

        val nameField = DashboardTextField(getString(Category.UTILITY, "autochannel_state0_mchannelname"), 1, AutoChannelCommand.MAX_CHANNEL_NAME_LENGTH) {
            DBAutoChannel.getInstance().retrieve(atomicGuild.idLong).nameMask = it.data
            return@DashboardTextField ActionResult()
        }
        nameField.value = DBAutoChannel.getInstance().retrieve(atomicGuild.idLong).nameMask
        mainContainer.add(nameField, DashboardText(getString(Category.UTILITY, "autochannel_vars")), DashboardSeparator())

        val lockedSwitch = DashboardSwitch(getString(Category.UTILITY, "autochannel_state0_mlocked")) {
            if (it.data != DBAutoChannel.getInstance().retrieve(guild.idLong).isLocked) {
                DBAutoChannel.getInstance().retrieve(guild.idLong).toggleLocked()
            }
            return@DashboardSwitch ActionResult()
        }
        lockedSwitch.isChecked = DBAutoChannel.getInstance().retrieve(atomicGuild.idLong).isLocked
        lockedSwitch.subtitle = getString(Category.UTILITY, "autochannel_dashboard_locked")
        mainContainer.add(lockedSwitch)
    }

}