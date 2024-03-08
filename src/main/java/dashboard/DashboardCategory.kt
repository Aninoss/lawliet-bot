package dashboard

import commands.Category
import commands.Command
import commands.CommandManager
import core.MemberCacheController
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicGuild
import core.atomicassets.AtomicMember
import core.cache.PatreonCache
import core.utils.BotPermissionUtil
import dashboard.component.DashboardText
import dashboard.container.DashboardContainer
import dashboard.container.VerticalContainer
import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONObject
import java.util.*
import kotlin.reflect.KClass

abstract class DashboardCategory(private val guildId: Long, private val userId: Long, val locale: Locale, guildEntity: GuildEntity) {

    val atomicGuild: AtomicGuild = AtomicGuild(guildId)
    val atomicMember: AtomicMember = AtomicMember(guildId, userId)
    val properties: DashboardProperties = this.javaClass.getAnnotation(DashboardProperties::class.java)

    var guildEntity: GuildEntity = guildEntity
        set(value) {
            val currentEntityManager = guildEntity.entityManager
            if (!currentEntityManager.isOpen) {
                field = value
            } else {
                value.entityManager.extendOther(currentEntityManager)
            }
        }
    val prefix: String = guildEntity.prefix
    val entityManager: EntityManagerWrapper
        get() = guildEntity.entityManager
    val isPremium
        get() = PatreonCache.getInstance().hasPremium(atomicMember.idLong, true) ||
                PatreonCache.getInstance().isUnlocked(atomicGuild.idLong)

    private var components: DashboardContainer? = null

    abstract fun retrievePageTitle(): String

    abstract fun generateComponents(guild: Guild, mainContainer: VerticalContainer)

    fun draw(): DashboardContainer {
        val mainContainer = VerticalContainer()
        components = mainContainer
        atomicGuild.get().ifPresent { guild ->
            if (anyCommandRequirementsAreAccessible()) {
                generateComponents(guild, mainContainer)
            } else {
                val dashboardText = DashboardText(getString(TextManager.GENERAL, "dashboard_noaccess"))
                dashboardText.style = DashboardText.Style.ERROR
                mainContainer.add(dashboardText)
            }
        }
        return components!!
    }

    fun receiveAction(action: JSONObject): ActionResult? {
        if (missingBotPermissions().isEmpty() && missingUserPermissions().isEmpty()) {
            return components?.receiveAction(action)
        } else {
            return null
        }
    }

    fun missingBotPermissions(): List<Permission> {
        return ShardManager.getLocalGuildById(guildId).orElse(null)?.let { guild ->
            return properties.botPermissions
                    .filter { !BotPermissionUtil.can(guild, it) }
        } ?: emptyList()
    }

    fun missingUserPermissions(): List<Permission> {
        return ShardManager.getLocalGuildById(guildId).orElse(null)?.let { guild ->
            return MemberCacheController.getInstance().loadMember(guild, userId).get()?.let { member ->
                return properties.userPermissions
                        .filter { !BotPermissionUtil.can(member, it) }
            } ?: emptyList()
        } ?: emptyList()
    }

    fun anyCommandRequirementsAreAccessible(): Boolean {
        if (properties.commandAccessRequirements.isEmpty()) {
            return true
        }

        val member = atomicMember.get().get()
        return properties.commandAccessRequirements
                .any {
                    CommandManager.commandIsTurnedOnEffectively(it.java, member, null)
                }
    }

    fun anyCommandsAreAccessible(vararg classes: KClass<out Command>): Boolean {
        if (classes.isEmpty()) {
            return true
        }

        val member = atomicMember.get().get()
        return classes
                .any {
                    CommandManager.commandIsTurnedOnEffectively(it.java, member, null)
                }
    }

    fun getString(category: String, key: String, vararg args: String): String {
        var text = TextManager.getString(locale, category, key, *args)
        text = text.replace("{PREFIX}", prefix)
        return text
    }

    fun getString(category: String, key: String, option: Int, vararg args: String): String {
        var text = TextManager.getString(locale, category, key, option, *args)
        text = text.replace("{PREFIX}", prefix)
        return text
    }

    fun getString(category: String, key: String, secondOption: Boolean, vararg args: String): String {
        var text = TextManager.getString(locale, category, key, secondOption, *args)
        text = text.replace("{PREFIX}", prefix)
        return text
    }

    fun getString(category: Category, key: String, vararg args: String): String {
        var text = TextManager.getString(locale, category, key, *args)
        text = text.replace("{PREFIX}", prefix)
        return text
    }

    fun getString(category: Category, key: String, option: Int, vararg args: String): String {
        var text = TextManager.getString(locale, category, key, option, *args)
        text = text.replace("{PREFIX}", prefix)
        return text
    }

    fun getString(category: Category, key: String, secondOption: Boolean, vararg args: String): String {
        var text = TextManager.getString(locale, category, key, secondOption, *args)
        text = text.replace("{PREFIX}", prefix)
        return text
    }

}