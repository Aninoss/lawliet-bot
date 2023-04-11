package dashboard.components

import commands.Command
import commands.CommandManager
import core.CustomObservableList
import core.MemberCacheController
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicRole
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import java.util.*
import kotlin.reflect.KClass

class DashboardMultiRolesComboBox(label: String, locale: Locale, guildId: Long, val memberId: Long, val selectedRoles: CustomObservableList<Long>,
                                  canBeEmpty: Boolean,
                                  max: Int, checkManageable: Boolean, commandAccessRequirement: KClass<out Command>? = null
) : DashboardComboBox(label, DataType.ROLES, canBeEmpty, max) {

    init {
        selectedValues = selectedRoles.map {
            val atomicRole = AtomicRole(guildId, it)
            DiscordEntity(it.toString(), atomicRole.name)
        }
        setActionListener { event ->
            val guild: Guild = ShardManager.getLocalGuildById(guildId).orElse(null) ?: return@setActionListener ActionResult()
            val role: Role? = guild.getRoleById(event.data.toLong())
            val member: Member = MemberCacheController.getInstance().loadMember(guild, memberId).get() ?: return@setActionListener ActionResult()

            if (role == null || !checkManageable || (BotPermissionUtil.canManage(member, role) && BotPermissionUtil.can(member, Permission.MANAGE_ROLES))) {
                if ((commandAccessRequirement != null && !CommandManager.commandIsTurnedOnEffectively(commandAccessRequirement.java, member, null)) ||
                    (role == null && event.type != "remove")
                ) {
                    return@setActionListener ActionResult()
                        .withRedraw()
                }

                if (event.type == "add") {
                    if (!checkManageable || (BotPermissionUtil.canManage(role) && BotPermissionUtil.can(guild.selfMember, Permission.MANAGE_ROLES))) {
                        selectedRoles.add(event.data.toLong())
                        return@setActionListener ActionResult()
                    } else {
                        val text = TextManager.getString(locale, TextManager.GENERAL, "permission_role", false, "\"${role!!.name}\"")
                        return@setActionListener ActionResult()
                            .withRedraw()
                            .withErrorMessage(text)
                    }
                } else if (event.type == "remove") {
                    selectedRoles.remove(event.data.toLong())
                    return@setActionListener ActionResult()
                } else {
                    return@setActionListener ActionResult()
                }
            } else {
                val text = TextManager.getString(locale, TextManager.GENERAL, "permission_role_user", false, "\"${role.name}\"")
                return@setActionListener ActionResult()
                    .withRedraw()
                    .withErrorMessage(text)
            }
        }
    }

}