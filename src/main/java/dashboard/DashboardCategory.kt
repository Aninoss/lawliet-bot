package dashboard

import java.util.*

abstract class DashboardCategory(val id: String) {

    abstract fun retrievePageTitle(locale: Locale): String

}