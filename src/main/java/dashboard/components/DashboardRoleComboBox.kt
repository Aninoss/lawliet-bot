package dashboard.components

import core.MemberCacheController
import core.TextManager
import core.atomicassets.AtomicRole
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import dashboard.listener.DashboardEventListener
import net.dv8tion.jda.api.Permission

class DashboardRoleComboBox(
    dashboardCategory: DashboardCategory,
    label: String,
    selectedRole: Long?,
    canBeEmpty: Boolean,
    checkManageable: Boolean,
    action: DashboardEventListener<String>
) : DashboardComboBox(label, DataType.ROLES, canBeEmpty, 1) {

    init {
        selectedValues = selectedRole?.let {
            val atomicRole = AtomicRole(dashboardCategory.atomicGuild.idLong, it)
            listOf(DiscordEntity(it.toString(), atomicRole.getName(dashboardCategory.locale)))
        } ?: emptyList<DiscordEntity>()

        setActionListener {
            if (it.data != null) {
                val guild = dashboardCategory.atomicGuild.get().get()
                val role = guild.getRoleById(it.data)!!
                val member = MemberCacheController.getInstance().loadMember(guild, dashboardCategory.atomicMember.idLong).get()
                if (member == null) {
                    return@setActionListener ActionResult()
                        .withRedraw()
                }

                if (checkManageable) {
                    if (!BotPermissionUtil.can(member, Permission.MANAGE_ROLES) || !BotPermissionUtil.canManage(member, role)) {
                        val text = TextManager.getString(dashboardCategory.locale, TextManager.GENERAL, "permission_role_user", false, "\"${role.name}\"")
                        return@setActionListener ActionResult()
                            .withRedraw()
                            .withErrorMessage(text)
                    }
                    if (!BotPermissionUtil.can(role.guild.selfMember, Permission.MANAGE_ROLES) || !BotPermissionUtil.canManage(role)) {
                        val text = TextManager.getString(dashboardCategory.locale, TextManager.GENERAL, "permission_role", false, "\"${role.name}\"")
                        return@setActionListener ActionResult()
                            .withRedraw()
                            .withErrorMessage(text)
                    }
                }
            }

            return@setActionListener action.accept(it)
        }
    }

}