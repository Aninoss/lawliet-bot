package dashboard.pages

import commands.Category
import commands.runnables.birthdaycategory.BirthdayConfigCommand
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
import mysql.hibernate.entity.guild.BirthdayEntity
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

    val birthdayEntity: BirthdayEntity
        get() = guildEntity.birthday

    override fun retrievePageTitle(): String {
        return getString(Category.BIRTHDAYS, "birthdayconfig_title")
    }

    override fun retrievePageDescription(): String {
        return getString(Category.BIRTHDAYS, "birthdayconfig_home_desc")
            .replace("`", "\"")
            .replace("{PREFIX}", prefix)
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val innerContainer = VerticalContainer()
        innerContainer.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.BIRTHDAYS, "birthdayconfig_home_active")) {
            birthdayEntity.beginTransaction()
            birthdayEntity.active = !birthdayEntity.active
            log(entityManager, BotLogEntity.Event.BIRTHDAY_CONFIG_ACTIVE, atomicMember, null, birthdayEntity.active)
            birthdayEntity.commitTransaction()
            return@DashboardSwitch ActionResult()
        }
        activeSwitch.isChecked = birthdayEntity.active
        innerContainer.add(activeSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.BIRTHDAYS, "birthdayconfig_home_channel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                birthdayEntity.channelId,
                true,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
        ) {
            birthdayEntity.beginTransaction()
            log(entityManager, BotLogEntity.Event.BIRTHDAY_CONFIG_CHANNEL, atomicMember, birthdayEntity.channelId, it.data)
            birthdayEntity.channelId = it.data?.toLong()
            birthdayEntity.commitTransaction()
            return@DashboardChannelComboBox ActionResult()
        }
        innerContainer.add(channelComboBox, DashboardSeparator())

        val roleComboBox = DashboardRoleComboBox(
            this,
            getString(Category.BIRTHDAYS, "birthdayconfig_home_role"),
            birthdayEntity.roleId,
            true,
            true
        ) {
            birthdayEntity.beginTransaction()
            log(entityManager, BotLogEntity.Event.BIRTHDAY_CONFIG_ROLE, atomicMember, birthdayEntity.roleId, it.data)
            birthdayEntity.roleId = it.data?.toLong()
            birthdayEntity.commitTransaction()
            return@DashboardRoleComboBox ActionResult()
        }
        innerContainer.add(roleComboBox)

        mainContainer.add(innerContainer)
    }

}