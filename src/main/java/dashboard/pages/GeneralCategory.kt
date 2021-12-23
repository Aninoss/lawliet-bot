package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.LanguageCommand
import commands.runnables.configurationcategory.PrefixCommand
import commands.runnables.utilitycategory.AutoQuoteCommand
import commands.runnables.utilitycategory.TriggerDeleteCommand
import constants.Language
import core.TextManager
import core.cache.PatreonCache
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardSelect
import dashboard.component.DashboardSwitch
import dashboard.component.DashboardTextField
import dashboard.container.HorizontalContainer
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import modules.Prefix
import mysql.modules.autoquote.DBAutoQuote
import mysql.modules.guild.DBGuild
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "general",
    userPermissions = [Permission.MANAGE_SERVER, Permission.MESSAGE_MANAGE],
    botPermissions = [Permission.MESSAGE_MANAGE]
)
class GeneralCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    override fun retrievePageTitle(): String {
        return TextManager.getString(locale, TextManager.GENERAL, "dashboard_general")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.add(
            generateTextFields(guild),
            generateAutoQuoteSwitch(),
            generateTriggerDeleteSwitch()
        )
    }

    private fun generateTextFields(guild: Guild): DashboardComponent {
        val container = HorizontalContainer()
        container.allowWrap = true

        val languageEntityList = Language.values().map { DiscordEntity(it.name, getString(Category.CONFIGURATION, "language_" + it.name)) }
        val languageSelect = DashboardSelect(Command.getCommandLanguage(LanguageCommand::class.java, locale).title, languageEntityList, false) {
            val language = Language.valueOf(it.data)
            DBGuild.getInstance().retrieve(atomicGuild.idLong).locale = language.locale
            ActionResult(false)
        }
        val locale = DBGuild.getInstance().retrieve(atomicGuild.idLong).locale
        val language = Language.from(locale)
        languageSelect.selectedValue = DiscordEntity(language.name, getString(Category.CONFIGURATION, "language_" + language.name))

        val prefixField = DashboardTextField(Command.getCommandLanguage(PrefixCommand::class.java, locale).title, 1, 5) {
            val prefix = it.data
            Prefix.changePrefix(guild, locale, prefix)
            ActionResult(false)
        }
        prefixField.value = DBGuild.getInstance().retrieve(atomicGuild.idLong).prefix

        container.add(languageSelect, prefixField)
        return container
    }

    private fun generateAutoQuoteSwitch(): DashboardComponent {
        val switch = DashboardSwitch(Command.getCommandLanguage(AutoQuoteCommand::class.java, locale).title) {
            DBAutoQuote.getInstance().retrieve(atomicGuild.idLong).isActive = it.data
            ActionResult(false)
        }
        switch.isChecked = DBAutoQuote.getInstance().retrieve(atomicGuild.idLong).isActive
        switch.subtitle = getString(Category.UTILITY, "autoquote_info")
        return switch
    }

    private fun generateTriggerDeleteSwitch(): DashboardComponent {
        val title = Command.getCommandLanguage(TriggerDeleteCommand::class.java, locale).title
        val switch = DashboardSwitch(getString(TextManager.GENERAL, "dashboard_premium", title)) {
            DBGuild.getInstance().retrieve(atomicGuild.idLong).isCommandAuthorMessageRemove = it.data
            ActionResult(false)
        }
        switch.isChecked = DBGuild.getInstance().retrieve(atomicGuild.idLong).isCommandAuthorMessageRemove
        switch.subtitle = getString(Category.UTILITY, "triggerdelete_info")
        switch.isEnabled = PatreonCache.getInstance().hasPremium(atomicMember.idLong, true) ||
                PatreonCache.getInstance().isUnlocked(atomicGuild.idLong)
        return switch
    }

}