package dashboard.pages

import commands.Category
import commands.runnables.configurationcategory.BirthdayConfigCommand
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardComboBox
import dashboard.component.DashboardSeparator
import dashboard.component.DashboardSwitch
import dashboard.components.DashboardChannelComboBox
import dashboard.components.DashboardRoleComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.BotLogEntity.Companion.log
import mysql.hibernate.entity.guild.BirthdayConfigEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "birthday",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [BirthdayConfigCommand::class]
)
class BirthdayCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    val birthdayConfigEntity: BirthdayConfigEntity
        get() = guildEntity.birthdayConfig

    override fun retrievePageTitle(): String {
        return getString(Category.CONFIGURATION, "birthdayconfig_title")
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "birthdayconfig_home_desc")
            .replace("`", "\"")
            .replace("{PREFIX}", prefix)
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val innerContainer = VerticalContainer()
        innerContainer.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "birthdayconfig_home_active")) {
            birthdayConfigEntity.beginTransaction()
            birthdayConfigEntity.active = !birthdayConfigEntity.active
            log(entityManager, BotLogEntity.Event.BIRTHDAY_CONFIG_ACTIVE, atomicMember, null, birthdayConfigEntity.active)
            birthdayConfigEntity.commitTransaction()
            return@DashboardSwitch ActionResult()
        }
        activeSwitch.isChecked = birthdayConfigEntity.active
        innerContainer.add(activeSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.CONFIGURATION, "birthdayconfig_home_channel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                birthdayConfigEntity.channelId,
                true,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
        ) {
            birthdayConfigEntity.beginTransaction()
            log(entityManager, BotLogEntity.Event.BIRTHDAY_CONFIG_CHANNEL, atomicMember, birthdayConfigEntity.channelId, it.data)
            birthdayConfigEntity.channelId = it.data?.toLong()
            birthdayConfigEntity.commitTransaction()
            return@DashboardChannelComboBox ActionResult()
        }
        innerContainer.add(channelComboBox, DashboardSeparator())

        val roleComboBox = DashboardRoleComboBox(
            this,
            getString(Category.CONFIGURATION, "birthdayconfig_home_role"),
            birthdayConfigEntity.roleId,
            true,
            true
        ) {
            birthdayConfigEntity.beginTransaction()
            log(entityManager, BotLogEntity.Event.BIRTHDAY_CONFIG_ROLE, atomicMember, birthdayConfigEntity.roleId, it.data)
            birthdayConfigEntity.roleId = it.data?.toLong()
            birthdayConfigEntity.commitTransaction()
            return@DashboardRoleComboBox ActionResult()
        }
        innerContainer.add(roleComboBox)

        mainContainer.add(innerContainer)
    }

}