package dashboard

import dashboard.pages.GeneralCategory
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object DashboardManager {

    val pageClasses: List<KClass<out DashboardCategory>>

    init {
        pageClasses = listOf(
            GeneralCategory::class
        )
    }

    @JvmStatic
    fun retrieveCategories(): List<DashboardCategory> {
        return pageClasses.map { it.createInstance() }
    }

}