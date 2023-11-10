package dashboard.pages

import commands.Category
import commands.runnables.configurationcategory.SuggestionConfigCommand
import core.TextManager
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardSeparator
import dashboard.component.DashboardSwitch
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.GuildEntity
import mysql.modules.suggestions.DBSuggestions
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "suggestions",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [SuggestionConfigCommand::class]
)
class SuggestionsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    override fun retrievePageTitle(): String {
        return getString(TextManager.GENERAL, "dashboard_suggestions")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "suggconfig_state0_mactive")) {
            if (it.data != DBSuggestions.getInstance().retrieve(guild.idLong).isActive) {
                DBSuggestions.getInstance().retrieve(guild.idLong).toggleActive()
            }
            ActionResult()
        }
        activeSwitch.isChecked = DBSuggestions.getInstance().retrieve(guild.idLong).isActive
        mainContainer.add(activeSwitch, DashboardSeparator())

        val channelComboBox = DashboardTextChannelComboBox(
                getString(Category.CONFIGURATION, "suggconfig_state0_mchannel"),
                locale,
                atomicGuild.idLong,
                DBSuggestions.getInstance().retrieve(guild.idLong).textChannelId.orElse(null),
                false,
        ) {
            val channel = atomicGuild.get().get().getTextChannelById(it.data)
            if (channel == null) {
                return@DashboardTextChannelComboBox ActionResult()
                        .withRedraw()
            }

            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) {
                return@DashboardTextChannelComboBox ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(TextManager.GENERAL, "permission_channel_history", "#${channel.getName()}"))
            }

            DBSuggestions.getInstance().retrieve(guild.idLong).setChannelId(it.data.toLong())
            return@DashboardTextChannelComboBox ActionResult()
        }
        mainContainer.add(channelComboBox)
    }

}