package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.StickyRolesCommand
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "stickyroles",
        userPermissions = [Permission.MANAGE_ROLES],
        botPermissions = [Permission.MANAGE_ROLES],
        commandAccessRequirements = [StickyRolesCommand::class]
)
class StickyRolesCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(StickyRolesCommand::class.java, locale).title
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "stickyroles_state0_description")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val innerContainer = VerticalContainer()
        innerContainer.isCard = true
        val rolesComboBox = DashboardMultiRolesComboBox(
                this,
                Command.getCommandLanguage(StickyRolesCommand::class.java, locale).title,
                { it.stickyRoles.roleIds },
                true,
                StickyRolesCommand.MAX_ROLES,
                true,
                null,
                BotLogEntity.Event.STICKY_ROLES
        )

        innerContainer.add(rolesComboBox)
        mainContainer.add(innerContainer)
    }

}