package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.StickyRolesCommand
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardText
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.GuildEntity
import mysql.modules.stickyroles.DBStickyRoles
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

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val descText = DashboardText(getString(Category.UTILITY, "stickyroles_state0_description"))
        val rolesComboBox = DashboardMultiRolesComboBox(
                this,
                Command.getCommandLanguage(StickyRolesCommand::class.java, locale).title,
                { DBStickyRoles.getInstance().retrieve(guild.idLong).roleIds },
                true,
                StickyRolesCommand.MAX_ROLES,
                true
        )

        mainContainer.add(descText, rolesComboBox)
    }

}