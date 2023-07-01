package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.LanguageCommand
import commands.runnables.configurationcategory.PrefixCommand
import commands.runnables.utilitycategory.AutoQuoteCommand
import commands.runnables.utilitycategory.TriggerDeleteCommand
import constants.Language
import core.TextManager
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardSelect
import dashboard.component.DashboardSeparator
import dashboard.component.DashboardSwitch
import dashboard.component.DashboardTextField
import dashboard.container.HorizontalContainer
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import modules.Prefix
import mysql.hibernate.EntityManagerWrapper
import mysql.modules.autoquote.DBAutoQuote
import mysql.modules.guild.DBGuild
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "general",
    userPermissions = [Permission.MANAGE_SERVER, Permission.MESSAGE_MANAGE],
    botPermissions = [Permission.MESSAGE_MANAGE],
    commandAccessRequirements = [LanguageCommand::class, PrefixCommand::class, AutoQuoteCommand::class, TriggerDeleteCommand::class]
)
class GeneralCategory(guildId: Long, userId: Long, locale: Locale, entityManager: EntityManagerWrapper) : DashboardCategory(guildId, userId, locale, entityManager) {

    override fun retrievePageTitle(): String {
        return getString(TextManager.GENERAL, "dashboard_general")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (anyCommandsAreAccessible(LanguageCommand::class, PrefixCommand::class)) {
            mainContainer.add(
                generateTextFields(guild),
                DashboardSeparator()
            )
        }

        if (anyCommandsAreAccessible(AutoQuoteCommand::class)) {
            mainContainer.add(
                generateAutoQuoteSwitch(),
                DashboardSeparator()
            )
        }

        if (anyCommandsAreAccessible(TriggerDeleteCommand::class)) {
            mainContainer.add(
                generateTriggerDeleteSwitch()
            )
        }
    }

    private fun generateTextFields(guild: Guild): DashboardComponent {
        val container = HorizontalContainer()
        container.allowWrap = true

        if (anyCommandsAreAccessible(LanguageCommand::class)) {
            val languageEntityList = Language.values().map { DiscordEntity(it.name, getString(Category.CONFIGURATION, "language_" + it.name)) }
            val languageSelect = DashboardSelect(Command.getCommandLanguage(LanguageCommand::class.java, locale).title, languageEntityList, false) {
                if (!anyCommandsAreAccessible(LanguageCommand::class)) {
                    return@DashboardSelect ActionResult()
                        .withRedraw()
                }

                val language = Language.valueOf(it.data)
                DBGuild.getInstance().retrieve(atomicGuild.idLong).locale = language.locale
                ActionResult()
            }
            val guildLocale = DBGuild.getInstance().retrieve(atomicGuild.idLong).locale
            val language = Language.from(guildLocale)
            languageSelect.selectedValue = DiscordEntity(language.name, getString(Category.CONFIGURATION, "language_" + language.name))
            container.add(languageSelect)
        }

        if (anyCommandsAreAccessible(PrefixCommand::class)) {
            val prefixField = DashboardTextField(Command.getCommandLanguage(PrefixCommand::class.java, locale).title, 1, 5) {
                if (!anyCommandsAreAccessible(PrefixCommand::class)) {
                    return@DashboardTextField ActionResult()
                        .withRedraw()
                }

                val prefix = it.data
                Prefix.changePrefix(guild, locale, prefix, entityManager)
                ActionResult()
            }
            prefixField.value = entityManager.findGuildEntity(atomicGuild.idLong).prefix
            container.add(prefixField)
        }

        return container
    }

    private fun generateAutoQuoteSwitch(): DashboardComponent {
        val switch = DashboardSwitch(Command.getCommandLanguage(AutoQuoteCommand::class.java, locale).title) {
            if (!anyCommandsAreAccessible(AutoQuoteCommand::class)) {
                return@DashboardSwitch ActionResult()
                    .withRedraw()
            }

            DBAutoQuote.getInstance().retrieve(atomicGuild.idLong).isActive = it.data
            ActionResult()
        }
        switch.isChecked = DBAutoQuote.getInstance().retrieve(atomicGuild.idLong).isActive
        switch.subtitle = getString(Category.UTILITY, "autoquote_info")
        return switch
    }

    private fun generateTriggerDeleteSwitch(): DashboardComponent {
        val title = Command.getCommandLanguage(TriggerDeleteCommand::class.java, locale).title
        val switch = DashboardSwitch(getString(TextManager.GENERAL, "dashboard_premium", title)) {
            if (!anyCommandsAreAccessible(TriggerDeleteCommand::class)) {
                return@DashboardSwitch ActionResult()
                    .withRedraw()
            }

            DBGuild.getInstance().retrieve(atomicGuild.idLong).isCommandAuthorMessageRemove = it.data
            ActionResult()
        }
        switch.isChecked = DBGuild.getInstance().retrieve(atomicGuild.idLong).isCommandAuthorMessageRemove
        switch.subtitle = getString(Category.UTILITY, "triggerdelete_info")
        switch.isEnabled = isPremium
        return switch
    }

}