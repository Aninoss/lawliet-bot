package dashboard

import com.google.common.cache.CacheBuilder
import dashboard.pages.*
import mysql.hibernate.EntityManagerWrapper
import java.time.Duration
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object DashboardManager {

    val pageClasses: List<KClass<out DashboardCategory>>

    @JvmStatic
    val categoryCache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofHours(4))
        .build<Long, DashboardCategory>()

    init {
        pageClasses = listOf(
            GeneralCategory::class,
            CommandManagementCategory::class,
            FisheryCategory::class,
            AlertsCategory::class,
            NSFWFilterCategory::class,
            ModerationCategory::class,
            InviteTrackingCategory::class,
            GiveawayCategory::class,
            ReactionRolesCategory::class,
            AutoRolesCategory::class,
            StickyRolesCategory::class,
            WelcomeCategory::class,
            TicketCategory::class,
            SuggestionsCategory::class,
            AutoChannelCategory::class,
            MemberCountDisplaysCategory::class,
        )
    }

    @JvmStatic
    fun retrieveCategories(guildId: Long, userId: Long, locale: Locale, entityManager: EntityManagerWrapper): List<DashboardCategory> {
        return pageClasses.map { it.primaryConstructor!!.call(guildId, userId, locale, entityManager) }
    }

    @JvmStatic
    fun retrieveCategory(categoryId: String, guildId: Long, userId: Long, locale: Locale, entityManager: EntityManagerWrapper): DashboardCategory {
        return pageClasses.map { it.primaryConstructor!!.call(guildId, userId, locale, entityManager) }
            .filter { it.properties.id == categoryId }[0]
    }

}