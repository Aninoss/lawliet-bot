package dashboard.pages

import commands.Category
import commands.runnables.configurationcategory.ApiCommand
import constants.ExternalLinks
import core.TextManager
import core.utils.RandomUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardButton
import dashboard.component.DashboardSeparator
import dashboard.component.DashboardText
import dashboard.container.HorizontalContainer
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "api",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [ApiCommand::class]
)
class ApiCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    private var showToken: Boolean = false

    override fun retrievePageTitle(): String {
        return getString(Category.CONFIGURATION, "api_title")
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "api_helptext")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val innerContainer = VerticalContainer()
        innerContainer.isCard = true
        innerContainer.add(DashboardText(getString(Category.CONFIGURATION, "api_default_desc").replace("*", "")), DashboardSeparator())

        val apiDefinitionLink = DashboardText(getString(Category.CONFIGURATION, "api_default_button_viewapidefinition"))
        apiDefinitionLink.url = ExternalLinks.API_DEFINITION_URL
        innerContainer.add(apiDefinitionLink)

        if (showToken) {
            innerContainer.add(DashboardText(getString(Category.CONFIGURATION, "api_default_auth_title") + ": " + guildEntity.apiTokenEffectively))
        }

        val buttonContainer = HorizontalContainer()
        buttonContainer.allowWrap = true

        val generateTokenButton = DashboardButton(getString(Category.CONFIGURATION, "api_default_button_generatetoken")) {
            guildEntity.beginTransaction()
            guildEntity.apiToken = RandomUtil.generateRandomString(30)
            BotLogEntity.log(entityManager, BotLogEntity.Event.API_TOKEN_NEW, atomicMember)
            guildEntity.commitTransaction()
            showToken = true
            ActionResult()
                .withRedraw()
                .withSuccessMessage(getString(Category.CONFIGURATION, "api_default_log_newtoken"))
        }
        if (guildEntity.apiTokenEffectively != null) {
            generateTokenButton.enableConfirmationMessage(getString(Category.CONFIGURATION, "api_dashboard_areyousure"))
        }
        generateTokenButton.isEnabled = isPremium
        buttonContainer.add(generateTokenButton)

        val removeTokenButton = DashboardButton(getString(Category.CONFIGURATION, "api_default_button_removetoken")) {
            guildEntity.beginTransaction()
            guildEntity.apiToken = null
            BotLogEntity.log(entityManager, BotLogEntity.Event.API_TOKEN_REMOVE, atomicMember)
            guildEntity.commitTransaction()
            showToken = false
            ActionResult()
                .withRedraw()
                .withSuccessMessage(getString(Category.CONFIGURATION, "api_default_log_removetoken"))
        }
        removeTokenButton.style = DashboardButton.Style.DANGER
        removeTokenButton.enableConfirmationMessage(getString(Category.CONFIGURATION, "api_dashboard_areyousure"))
        removeTokenButton.isEnabled = isPremium
        buttonContainer.add(removeTokenButton)

        innerContainer.add(buttonContainer)
        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            innerContainer.add(text)
        }
        mainContainer.add(innerContainer)
    }

}