package dashboard.pages

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.configurationcategory.CommandManagementCommand
import commands.runnables.configurationcategory.WhiteListCommand
import core.TextManager
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardComboBox
import dashboard.component.DashboardText
import dashboard.component.DashboardTitle
import dashboard.components.DashboardMultiTextChannelsComboBox
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import mysql.modules.commandmanagement.CommandManagementData
import mysql.modules.commandmanagement.DBCommandManagement
import mysql.modules.whitelistedchannels.DBWhiteListedChannels
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "commandmanagement",
    userPermissions = [Permission.ADMINISTRATOR]
)
class CommandManagementCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(CommandManagementCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.add(
            generateCommandManagementField(guild),
            generateChannelWhitelistField(guild)
        )
    }

    private fun generateChannelWhitelistField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        val whitelistText = Command.getCommandLanguage(WhiteListCommand::class.java, locale).title
        container.add(
            DashboardTitle(whitelistText),
            DashboardText(getString(Category.CONFIGURATION, "whitelist_state0_description")),
            DashboardMultiTextChannelsComboBox(guild.idLong, DBWhiteListedChannels.getInstance().retrieve(guild.idLong).channelIds, true, WhiteListCommand.MAX_CHANNELS)
        )
        return container
    }

    private fun generateCommandManagementField(guild: Guild): DashboardComponent {
        val commandManagementData = DBCommandManagement.getInstance().retrieve(guild.idLong)
        val container = VerticalContainer()
        container.add(DashboardText(getString(Category.CONFIGURATION, "cman_state0_desc")))

        val commandCategoryValues = Category.independentValues().map { DiscordEntity(it.id, getString(TextManager.COMMANDS, it.id)) }
        container.add(generateBlacklistComboBox(commandManagementData, getString(Category.CONFIGURATION, "cman_state0_mcategories"), commandCategoryValues))

        val commandValues = CommandContainer.getFullCommandList()
            .map {
                val trigger = Command.getCommandProperties(it).trigger
                DiscordEntity(trigger, trigger)
            }
            .sortedBy { it.id }
        container.add(generateBlacklistComboBox(commandManagementData, getString(Category.CONFIGURATION, "cman_state0_mcommands"), commandValues))
        return container
    }

    private fun generateBlacklistComboBox(commandManagementData: CommandManagementData, label: String, values: List<DiscordEntity>): DashboardComponent {
        val comboBox = DashboardComboBox(label, values, true, Int.MAX_VALUE) {
            if (it.type == "add") {
                commandManagementData.switchedOffElements += it.data
            } else if (it.type == "remove") {
                commandManagementData.switchedOffElements -= it.data
            }
            ActionResult(false)
        }
        comboBox.selectedValues = values.filter { commandManagementData.switchedOffElements.contains(it.id) }
        return comboBox
    }

}