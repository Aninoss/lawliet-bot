package dashboard.pages

import commands.Category
import commands.runnables.configurationcategory.SuggestionConfigCommand
import core.TextManager
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardComboBox
import dashboard.component.DashboardSeparator
import dashboard.component.DashboardSwitch
import dashboard.components.DashboardChannelComboBox
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.BotLogEntity.Companion.log
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.suggestions.DBSuggestions
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
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
            val suggestionsData = DBSuggestions.getInstance().retrieve(guild.idLong)
            if (it.data != suggestionsData.isActive) {
                suggestionsData.toggleActive()

                entityManager.transaction.begin()
                log(entityManager, BotLogEntity.Event.SERVER_SUGGESTIONS_ACTIVE, atomicMember, null, suggestionsData.isActive)
                entityManager.transaction.commit()
            }
            ActionResult()
        }
        activeSwitch.isChecked = DBSuggestions.getInstance().retrieve(guild.idLong).isActive
        mainContainer.add(activeSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.CONFIGURATION, "suggconfig_state0_mchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                DBSuggestions.getInstance().retrieve(guild.idLong).channelId.orElse(null),
                false,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)
        ) {
            val suggestionsData = DBSuggestions.getInstance().retrieve(guild.idLong)

            entityManager.transaction.begin()
            log(entityManager, BotLogEntity.Event.SERVER_SUGGESTIONS_CHANNEL, atomicMember, suggestionsData.channelId.orElse(null), it.data.toLong())
            entityManager.transaction.commit()

            suggestionsData.setChannelId(it.data.toLong())
            return@DashboardChannelComboBox ActionResult()
        }
        mainContainer.add(channelComboBox)
    }

}