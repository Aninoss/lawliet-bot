package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.AutoChannelCommand
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardMultiChannelsComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.AutoChannelEntity
import mysql.hibernate.entity.guild.GuildEntity
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

    val autoChannelEntity: AutoChannelEntity
        get() = guildEntity.autoChannel

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(AutoChannelCommand::class.java, locale).title
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "autochannel_state0_description")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val innerContainer = VerticalContainer()
        innerContainer.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "autochannel_state0_mactive")) {
            autoChannelEntity.beginTransaction()
            autoChannelEntity.active = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.AUTO_CHANNEL_ACTIVE, atomicMember, null, autoChannelEntity.active)
            autoChannelEntity.commitTransaction()
            return@DashboardSwitch ActionResult()
        }
        activeSwitch.isChecked = autoChannelEntity.active
        innerContainer.add(activeSwitch, DashboardSeparator())

        val parentChannelsComboBox = DashboardMultiChannelsComboBox(
            this,
            getString(Category.CONFIGURATION, "autochannel_state0_mchannel"),
            DashboardComboBox.DataType.VOICE_CHANNELS,
            { it.autoChannel.parentChannelIds },
            true,
            AutoChannelCommand.MAX_PARENT_CHANNELS,
            null,
            BotLogEntity.Event.AUTO_CHANNEL_INITIAL_VOICE_CHANNELS,
            arrayOf(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS),
            arrayOf(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)
        )
        innerContainer.add(parentChannelsComboBox, DashboardSeparator())

        val nameField = DashboardTextField(getString(Category.CONFIGURATION, "autochannel_state0_mchannelname"), 1, AutoChannelCommand.MAX_CHANNEL_NAME_LENGTH) {
            autoChannelEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.AUTO_CHANNEL_NEW_CHANNEL_NAME, atomicMember, autoChannelEntity.nameMask, it.data)
            autoChannelEntity.nameMask = it.data
            autoChannelEntity.commitTransaction()
            return@DashboardTextField ActionResult()
        }
        nameField.value = autoChannelEntity.nameMask
        innerContainer.add(
                nameField,
                DashboardText(getString(Category.CONFIGURATION, "autochannel_vars").replace("- ", ""), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val lockedSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "autochannel_state0_mlocked")) {
            autoChannelEntity.beginTransaction()
            autoChannelEntity.beginLocked = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.AUTO_CHANNEL_BEGIN_LOCKED, atomicMember, null, autoChannelEntity.beginLocked)
            autoChannelEntity.commitTransaction()
            return@DashboardSwitch ActionResult()
        }
        lockedSwitch.isChecked = autoChannelEntity.beginLocked
        lockedSwitch.subtitle = getString(Category.CONFIGURATION, "autochannel_dashboard_locked")
        innerContainer.add(lockedSwitch)
        mainContainer.add(innerContainer)
    }

}