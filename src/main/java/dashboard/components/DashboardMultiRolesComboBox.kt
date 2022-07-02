package dashboard.components

import core.CustomObservableList
import core.MemberCacheController
import core.TextManager
import core.atomicassets.AtomicRole
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import java.util.*

class DashboardMultiRolesComboBox(label: String, locale: Locale, guildId: Long, val memberId: Long, val selectedRoles: CustomObservableList<Long>, canBeEmpty: Boolean,
                                  max: Int, checkManageable: Boolean
) : DashboardComboBox(label, DataType.ROLES, canBeEmpty, max) {

    constructor(locale: Locale, guildId: Long, memberId: Long, selectedRoles: CustomObservableList<Long>, canBeEmpty: Boolean, max: Int, checkManageable: Boolean) :
            this("", locale, guildId, memberId, selectedRoles, canBeEmpty, max, checkManageable)

    init {
        selectedValues = selectedRoles.map {
            val atomicRole = AtomicRole(guildId, it)
            DiscordEntity(it.toString(), atomicRole.name)
        }
        setActionListener { event ->
            val role: Role? = AtomicRole(guildId, event.data.toLong()).get().orElse(null)
            val member: Member? = role?.let { MemberCacheController.getInstance().loadMember(it.guild, memberId).get() }
            if (member != null) {
                if (BotPermissionUtil.canManage(member, role) && BotPermissionUtil.can(member, Permission.MANAGE_ROLES)) {
                    if (event.type == "add") {
                        if (!checkManageable || (BotPermissionUtil.canManage(role) && BotPermissionUtil.can(role.guild.selfMember, Permission.MANAGE_ROLES))) {
                            selectedRoles.add(event.data.toLong())
                            return@setActionListener ActionResult()
                        } else {
                            val text = TextManager.getString(locale, TextManager.GENERAL, "permission_role", false, "\"${role.name}\"")
                            return@setActionListener ActionResult()
                                .withRedraw()
                                .withErrorMessage(text)
                        }
                    } else if (event.type == "remove") {
                        selectedRoles.remove(event.data.toLong())
                        return@setActionListener ActionResult()
                    }
                } else {
                    val text = TextManager.getString(locale, TextManager.GENERAL, "permission_role_user", false, "\"${role.name}\"")
                    return@setActionListener ActionResult()
                        .withRedraw()
                        .withErrorMessage(text)
                }
            }
            ActionResult()
        }
    }

}