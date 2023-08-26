package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.CustomConfigCommand
import core.TextManager
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardText
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "customcommands"
)
class CustomCommandsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    override fun retrievePageTitle(): String {
        return getString(Category.UTILITY, "customconfig_dashboard_title")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val text = getString(TextManager.GENERAL, "dashboard_wip", Command.getCommandProperties(CustomConfigCommand::class.java).trigger)
        mainContainer.add(DashboardText(text))
    }

}