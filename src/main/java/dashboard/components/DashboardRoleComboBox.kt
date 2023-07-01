package dashboard.components

import core.MemberCacheController
import core.TextManager
import core.atomicassets.AtomicRole
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import java.util.*

class DashboardRoleComboBox(label: String, locale: Locale, guildId: Long, val memberId: Long, selectedRole: Long?, canBeEmpty: Boolean,
                            checkManageable: Boolean, actionListener: DashboardEventListener<String>
) : DashboardComboBox(label, DataType.ROLES, canBeEmpty, 1) {

    init {
        selectedValues = selectedRole?.let {
            val atomicRole = AtomicRole(guildId, it)
            listOf(DiscordEntity(it.toString(), atomicRole.getName(locale)))
        } ?: emptyList<DiscordEntity>()

        setActionListener { event ->
            val role: Role? = AtomicRole(guildId, event.data.toLong()).get().orElse(null)
            val member: Member? = role?.let { MemberCacheController.getInstance().loadMember(it.guild, memberId).get() }
            if (member != null) {
                if (!checkManageable || (BotPermissionUtil.canManage(member, role) && BotPermissionUtil.can(member, Permission.MANAGE_ROLES))) {
                    if (event.data != null &&
                        checkManageable &&
                        (!BotPermissionUtil.canManage(role) || !BotPermissionUtil.can(role.guild.selfMember, Permission.MANAGE_ROLES))
                    ) {
                        val text = TextManager.getString(locale, TextManager.GENERAL, "permission_role", false, "\"${role.name}\"")
                        return@setActionListener ActionResult()
                            .withRedraw()
                            .withErrorMessage(text)
                    }
                    return@setActionListener actionListener.accept(event)
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