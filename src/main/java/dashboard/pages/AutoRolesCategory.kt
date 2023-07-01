package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.AutoRolesCommand
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardText
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.GuildEntity
import mysql.modules.autoroles.DBAutoRoles
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "autoroles",
    userPermissions = [Permission.MANAGE_ROLES],
    botPermissions = [Permission.MANAGE_ROLES],
    commandAccessRequirements = [AutoRolesCommand::class]
)
class AutoRolesCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(AutoRolesCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val descText = DashboardText(getString(Category.UTILITY, "autoroles_state0_description"))
        val rolesComboBox = DashboardMultiRolesComboBox(
            Command.getCommandLanguage(AutoRolesCommand::class.java, locale).title,
            locale,
            guild.idLong,
            atomicMember.idLong,
            DBAutoRoles.getInstance().retrieve(guild.idLong).roleIds,
            true,
            AutoRolesCommand.MAX_ROLES,
            true
        )

        mainContainer.add(descText, rolesComboBox)
    }

}