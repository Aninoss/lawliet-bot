package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.NSFWFilterCommand
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardComboBox
import dashboard.component.DashboardText
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import mysql.modules.nsfwfilter.DBNSFWFilters
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "nsfw",
    userPermissions = [Permission.MANAGE_SERVER],
    commandAccessRequirements = [NSFWFilterCommand::class]
)
class NSFWFilterCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(NSFWFilterCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.add(
            DashboardText(Command.getCommandLanguage(NSFWFilterCommand::class.java, locale).descShort),
            generateFilterComboBox(guild)
        )
    }

    private fun generateFilterComboBox(guild: Guild): DashboardComponent {
        val nsfwKeywords = DBNSFWFilters.getInstance().retrieve(guild.idLong).keywords
        val label = getString(Category.CONFIGURATION, "nsfwfilter_state0_mkeywords")
        val comboBox = DashboardComboBox(label, emptyList(), true, NSFWFilterCommand.MAX_FILTERS) {
            if (it.type == "add") {
                it.data.split(" ").forEach { data ->
                    if (data.length <= NSFWFilterCommand.MAX_LENGTH && !nsfwKeywords.contains(data) && data.length > 0) {
                        nsfwKeywords.add(data.lowercase())
                    }
                }
            } else if (it.type == "remove") {
                nsfwKeywords.remove(it.data)
            }
            ActionResult()
                .withRedraw()
        }
        comboBox.allowCustomValues = true
        comboBox.selectedValues = nsfwKeywords.map { DiscordEntity(it, it) }
        return comboBox
    }

}