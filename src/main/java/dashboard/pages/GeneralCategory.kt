package dashboard.pages

import core.TextManager
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardButton
import dashboard.component.DashboardText
import dashboard.container.DashboardContainer
import dashboard.container.VerticalContainer
import java.util.*

@DashboardProperties(
    id = "general"
)
class GeneralCategory(guildId: Long, userId: Long, locale: Locale): DashboardCategory(guildId, userId, locale) {

    override fun retrievePageTitle(): String {
        return TextManager.getString(locale, TextManager.GENERAL, "general")
    }

    override fun generateComponents(): DashboardContainer {
        val mainContainer = VerticalContainer()
        val button = DashboardButton("Test") {
            println("TEST")
            ActionResult(true)
                .withSuccessMessage("YAY")
        }
        mainContainer.add(button)
        mainContainer.add(DashboardText(Random().nextInt().toString()))
        return mainContainer
    }

}