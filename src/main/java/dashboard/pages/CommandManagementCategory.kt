package dashboard.pages

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.configurationcategory.CommandManagementCommand
import commands.runnables.configurationcategory.CommandPermissionsCommand
import commands.runnables.configurationcategory.WhiteListCommand
import core.CommandPermissions
import core.TextManager
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardMultiTextChannelsComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
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
    userPermissions = [Permission.ADMINISTRATOR],
    commandAccessRequirements = [CommandManagementCommand::class, WhiteListCommand::class, CommandPermissionsCommand::class]
)
class CommandManagementCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    override fun retrievePageTitle(): String {
        return getString(TextManager.GENERAL, "dashboard_cman")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (anyCommandsAreAccessible(CommandManagementCommand::class)) {
            mainContainer.add(generateCommandManagementField(guild))
        }

        if (anyCommandsAreAccessible(WhiteListCommand::class)) {
            mainContainer.add(generateChannelWhitelistField(guild))
        }

        if (anyCommandsAreAccessible(CommandPermissionsCommand::class)) {
            mainContainer.add(generateCommandPermissionsField(guild))
        }
    }

    private fun generateCommandPermissionsField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        val commandPermissionsText = Command.getCommandLanguage(CommandPermissionsCommand::class.java, locale).title
        container.add(
            DashboardTitle(commandPermissionsText),
            DashboardText(getString(Category.CONFIGURATION, "cperms_message0").replace("**", "") + "\n" + getString(Category.CONFIGURATION, "cperms_message1")),
        )

        val button = DashboardButton(getString(Category.CONFIGURATION, "cperms_button")) {
            if (!anyCommandsAreAccessible(CommandPermissionsCommand::class)) {
                return@DashboardButton ActionResult()
                    .withRedraw()
            }

            val actionResult = ActionResult()
            if (CommandPermissions.transferCommandPermissions(guild)) {
                actionResult.withSuccessMessage(getString(Category.CONFIGURATION, "cperms_success"))
            } else {
                actionResult.withErrorMessage(getString(Category.CONFIGURATION, "cperms_failed"))
            }
        }
        button.style = DashboardButton.Style.PRIMARY
        val buttonField = HorizontalContainer()
        buttonField.add(button, HorizontalPusher())
        container.add(DashboardSeparator(), buttonField)

        return container
    }

    private fun generateChannelWhitelistField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        val whitelistText = Command.getCommandLanguage(WhiteListCommand::class.java, locale).title
        container.add(
            DashboardTitle(whitelistText),
            DashboardText(getString(Category.CONFIGURATION, "whitelist_state0_description"))
        )

        val obsoleteWarning = DashboardText(getString(Category.CONFIGURATION, "cperms_obsolete_dashboard"))
        obsoleteWarning.style = DashboardText.Style.ERROR
        container.add(obsoleteWarning)

        container.add(
            DashboardMultiTextChannelsComboBox(
                "", guild.idLong, DBWhiteListedChannels.getInstance().retrieve(guild.idLong).channelIds,
                true, WhiteListCommand.MAX_CHANNELS, atomicMember.idLong, WhiteListCommand::class
            )
        )

        return container
    }

    private fun generateCommandManagementField(guild: Guild): DashboardComponent {
        val commandManagementData = DBCommandManagement.getInstance().retrieve(guild.idLong)
        val container = VerticalContainer()
        container.add(DashboardText(getString(Category.CONFIGURATION, "cman_state0_desc")))

        val obsoleteWarning = DashboardText(getString(Category.CONFIGURATION, "cperms_obsolete_dashboard"))
        obsoleteWarning.style = DashboardText.Style.ERROR
        container.add(obsoleteWarning)

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

    private fun generateBlacklistComboBox(commandManagementData: CommandManagementData, label: String, values: List<DiscordEntity>
    ): DashboardComponent {
        val comboBox = DashboardComboBox(label, values, true, Int.MAX_VALUE) {
            if (!anyCommandsAreAccessible(CommandManagementCommand::class)) {
                return@DashboardComboBox ActionResult()
                    .withRedraw()
            }

            if (it.type == "add") {
                commandManagementData.switchedOffElements += it.data
            } else if (it.type == "remove") {
                commandManagementData.switchedOffElements -= it.data
            }
            ActionResult()
        }
        comboBox.selectedValues = values.filter { commandManagementData.switchedOffElements.contains(it.id) }
        return comboBox
    }

}