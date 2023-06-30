package dashboard.pages

import commands.Command
import commands.runnables.configurationcategory.SuggestionConfigCommand
import core.TextManager
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardText
import dashboard.container.VerticalContainer
import mysql.hibernate.EntityManagerWrapper
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "suggestions"
)
class SuggestionsCategory(guildId: Long, userId: Long, locale: Locale, entityManager: EntityManagerWrapper) : DashboardCategory(guildId, userId, locale, entityManager) {

    override fun retrievePageTitle(): String {
        return getString(TextManager.GENERAL, "dashboard_suggestions")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val text = getString(TextManager.GENERAL, "dashboard_wip", Command.getCommandProperties(SuggestionConfigCommand::class.java).trigger)
        mainContainer.add(DashboardText(text))
    }

}