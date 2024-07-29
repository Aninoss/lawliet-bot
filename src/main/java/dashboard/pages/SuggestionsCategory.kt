package dashboard.pages

import commands.Category
import commands.runnables.configurationcategory.SuggestionConfigCommand
import commands.runnables.configurationcategory.SuggestionManageCommand
import core.TextManager
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardChannelComboBox
import dashboard.components.PageField
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import modules.suggestions.SuggestionMessage
import modules.suggestions.Suggestions
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.BotLogEntity.Companion.log
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.suggestions.DBSuggestions
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*
import kotlin.math.min

@DashboardProperties(
        id = "suggestions",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [SuggestionConfigCommand::class, SuggestionManageCommand::class]
)
class SuggestionsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var manageIndex = 0

    override fun retrievePageTitle(): String {
        return getString(TextManager.GENERAL, "dashboard_suggestions")
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "suggconfig_state0_description")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (anyCommandsAreAccessible(SuggestionConfigCommand::class)) {
            mainContainer.add(generateMainComponents())
        }
        if (anyCommandsAreAccessible(SuggestionManageCommand::class)) {
            mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, "suggmanage_title")),
                generateManageComponents()
            )
        }
    }

    fun generateMainComponents(): DashboardComponent {
        val innerContainer = VerticalContainer()
        innerContainer.isCard = true

        val activeSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "suggconfig_state0_mactive")) {
            val suggestionsData = DBSuggestions.getInstance().retrieve(atomicGuild.idLong)
            if (it.data != suggestionsData.isActive) {
                suggestionsData.toggleActive()

                entityManager.transaction.begin()
                log(entityManager, BotLogEntity.Event.SERVER_SUGGESTIONS_ACTIVE, atomicMember, null, suggestionsData.isActive)
                entityManager.transaction.commit()
            }
            ActionResult()
        }
        activeSwitch.isChecked = DBSuggestions.getInstance().retrieve(atomicGuild.idLong).isActive
        innerContainer.add(activeSwitch, DashboardSeparator())

        val channelComboBox = DashboardChannelComboBox(
            this,
            getString(Category.CONFIGURATION, "suggconfig_state0_mchannel"),
            DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
            DBSuggestions.getInstance().retrieve(atomicGuild.idLong).channelId.orElse(null),
            false,
            arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)
        ) {
            val suggestionsData = DBSuggestions.getInstance().retrieve(atomicGuild.idLong)

            entityManager.transaction.begin()
            log(entityManager, BotLogEntity.Event.SERVER_SUGGESTIONS_CHANNEL, atomicMember, suggestionsData.channelId.orElse(null), it.data.toLong())
            entityManager.transaction.commit()

            suggestionsData.setChannelId(it.data.toLong())
            return@DashboardChannelComboBox ActionResult()
        }
        innerContainer.add(channelComboBox)
        return innerContainer
    }

    fun generateManageComponents(): DashboardComponent {
        val suggestions: List<SuggestionMessage> = DBSuggestions.getInstance().retrieve(atomicGuild.idLong).suggestionMessages.values
            .sortedBy { it.messageId }
        if (suggestions.isEmpty()) {
            return DashboardText(getString(TextManager.GENERAL, "empty"))
        }

        manageIndex = min(suggestions.size - 1, manageIndex)
        val suggestion = suggestions[manageIndex]
        Suggestions.refreshSuggestionMessage(suggestion)

        val innerContainer = VerticalContainer(DashboardText(suggestion.content))
        innerContainer.isCard = true

        val valuesContainer = HorizontalContainer()
        valuesContainer.add(
            DashboardText(Suggestions.getAuthorString(locale, suggestion), DashboardText.Style.HINT),
            HorizontalPusher(),
            DashboardText(getString(Category.CONFIGURATION, "suggmanage_dashboard_approval", Suggestions.getApprovalRatio(suggestion)), DashboardText.Style.HINT)
        )
        innerContainer.add(valuesContainer)

        val acceptButton = DashboardButton(getString(Category.CONFIGURATION, "suggmanage_button_accept")) { process(suggestion, true) }
        val declineButton = DashboardButton(getString(Category.CONFIGURATION, "suggmanage_button_decline")) { process(suggestion, false) }
        innerContainer.add(
            HorizontalContainer(acceptButton, declineButton, HorizontalPusher()),
            DashboardSeparator(),
            PageField(manageIndex, suggestions.size, 1) { manageIndex = it }
        )
        return innerContainer
    }

    private fun process(suggestionMessage: SuggestionMessage, accept: Boolean): ActionResult {
        val err = Suggestions.processSuggestion(locale, suggestionMessage, accept)
        if (err == null) {
            entityManager.transaction.begin()
            log(entityManager, if (accept) BotLogEntity.Event.SERVER_SUGGESTIONS_MANAGE_ACCEPT else BotLogEntity.Event.SERVER_SUGGESTIONS_MANAGE_DECLINE, atomicMember, suggestionMessage.messageId)
            entityManager.transaction.commit()

            return ActionResult()
                .withSuccessMessage(getString(Category.CONFIGURATION, if (accept) "suggmanage_log_accepted" else "suggmanage_log_declined"))
                .withRedraw()
        } else {
            return ActionResult()
                .withErrorMessage(err)
                .withRedraw()
        }
    }

}