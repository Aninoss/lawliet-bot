package dashboard

import com.google.common.cache.CacheBuilder
import dashboard.pages.*
import mysql.hibernate.entity.guild.GuildEntity
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
                CustomCommandsCategory::class,
                CommandChannelShortcutsCategory::class
        )
    }

    @JvmStatic
    fun retrieveCategories(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity): List<DashboardCategory> {
        return pageClasses.map { it.primaryConstructor!!.call(guildId, userId, locale, guildEntity) }
    }

    @JvmStatic
    fun retrieveCategory(categoryId: String, guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity): DashboardCategory {
        return pageClasses.map { it.primaryConstructor!!.call(guildId, userId, locale, guildEntity) }
                .filter { it.properties.id == categoryId }[0]
    }

}