package dashboard.pages

import core.TextManager
import dashboard.DashboardCategory
import java.util.*

class GeneralCategory: DashboardCategory("general") {

    override fun retrievePageTitle(locale: Locale): String {
        return TextManager.getString(locale, TextManager.GENERAL, "general")
    }

}