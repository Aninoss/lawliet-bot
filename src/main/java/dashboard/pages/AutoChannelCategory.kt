package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.AutoChannelCommand
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardChannelComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
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
        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "autochannel_state0_mactive")) {
            if (it.data != DBAutoChannel.getInstance().retrieve(guild.idLong).isActive) {
                val autoChannelData = DBAutoChannel.getInstance().retrieve(guild.idLong)
                autoChannelData.toggleActive()

                entityManager.transaction.begin()
                BotLogEntity.log(entityManager, BotLogEntity.Event.AUTO_CHANNEL_ACTIVE, atomicMember, null, autoChannelData.isActive)
                entityManager.transaction.commit()
            }
            return@DashboardSwitch ActionResult()
        }
        activeSwitch.isChecked = DBAutoChannel.getInstance().retrieve(guild.idLong).isActive
        mainContainer.add(activeSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.CONFIGURATION, "autochannel_state0_mchannel"),
                DashboardComboBox.DataType.VOICE_CHANNELS,
                DBAutoChannel.getInstance().retrieve(guild.idLong).parentChannelId.orElse(null),
                false,
        ) {
            val channel = atomicGuild.get().get().getVoiceChannelById(it.data)
            if (channel == null) {
                return@DashboardChannelComboBox ActionResult()
                        .withRedraw()
            }

            val channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(locale, channel, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)
            if (channelMissingPerms != null) {
                return@DashboardChannelComboBox ActionResult()
                        .withErrorMessage(channelMissingPerms)
                        .withRedraw()
            }

            val parent = channel.getParentCategory()
            if (parent != null) {
                val categoryMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(locale, parent, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)
                if (categoryMissingPerms != null) {
                    return@DashboardChannelComboBox ActionResult()
                            .withErrorMessage(categoryMissingPerms)
                            .withRedraw()
                }
            }

            val autoChannelData = DBAutoChannel.getInstance().retrieve(guild.idLong)
            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.AUTO_CHANNEL_INITIAL_VOICE_CHANNEL, atomicMember, autoChannelData.parentChannelId.orElse(null), it.data.toLong())
            entityManager.transaction.commit()

            autoChannelData.setParentChannelId(it.data.toLong())
            return@DashboardChannelComboBox ActionResult()
        }
        mainContainer.add(channelComboBox, DashboardSeparator())

        val nameField = DashboardTextField(getString(Category.CONFIGURATION, "autochannel_state0_mchannelname"), 1, AutoChannelCommand.MAX_CHANNEL_NAME_LENGTH) {
            val autoChannelData = DBAutoChannel.getInstance().retrieve(atomicGuild.idLong)

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.AUTO_CHANNEL_NEW_CHANNEL_NAME, atomicMember, autoChannelData.nameMask, it.data)
            entityManager.transaction.commit()

            autoChannelData.nameMask = it.data
            return@DashboardTextField ActionResult()
        }
        nameField.value = DBAutoChannel.getInstance().retrieve(atomicGuild.idLong).nameMask
        mainContainer.add(nameField, DashboardText(getString(Category.CONFIGURATION, "autochannel_vars")), DashboardSeparator())

        val lockedSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "autochannel_state0_mlocked")) {
            if (it.data != DBAutoChannel.getInstance().retrieve(guild.idLong).isLocked) {
                val autoChannelData = DBAutoChannel.getInstance().retrieve(guild.idLong)
                autoChannelData.toggleLocked()

                entityManager.transaction.begin()
                BotLogEntity.log(entityManager, BotLogEntity.Event.AUTO_CHANNEL_BEGIN_LOCKED, atomicMember, null, autoChannelData.isLocked)
                entityManager.transaction.commit()
            }
            return@DashboardSwitch ActionResult()
        }
        lockedSwitch.isChecked = DBAutoChannel.getInstance().retrieve(atomicGuild.idLong).isLocked
        lockedSwitch.subtitle = getString(Category.CONFIGURATION, "autochannel_dashboard_locked")
        mainContainer.add(lockedSwitch)
    }

}