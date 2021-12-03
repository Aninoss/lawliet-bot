package dashboard

import core.MemberCacheController
import core.ShardManager
import core.utils.BotPermissionUtil
import dashboard.container.DashboardContainer
import net.dv8tion.jda.api.Permission
import org.json.JSONObject
import java.util.*

abstract class DashboardCategory(val guildId: Long, val userId: Long, val locale: Locale) {

    val properties: DashboardProperties
        get() = this.javaClass.getAnnotation(DashboardProperties::class.java)

    private var components: DashboardContainer? = null

    abstract fun retrievePageTitle(): String

    abstract fun generateComponents(): DashboardContainer

    fun draw(): DashboardContainer {
        components = generateComponents()
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

}