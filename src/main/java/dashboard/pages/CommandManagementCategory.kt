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
import dashboard.component.DashboardButton
import dashboard.component.DashboardComboBox
import dashboard.component.DashboardText
import dashboard.component.DashboardTitle
import dashboard.components.DashboardMultiChannelsComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "commandmanagement",
        userPermissions = [Permission.ADMINISTRATOR],
        commandAccessRequirements = [CommandManagementCommand::class, WhiteListCommand::class, CommandPermissionsCommand::class]
)
class CommandManagementCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) :
        DashboardCategory(guildId, userId, locale, guildEntity) {

    override fun retrievePageTitle(): String {
        return getString(TextManager.GENERAL, "dashboard_cman")
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "cman_state0_desc")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (anyCommandsAreAccessible(CommandManagementCommand::class)) {
            mainContainer.add(generateCommandManagementField())
        }

        if (anyCommandsAreAccessible(WhiteListCommand::class)) {
            val whitelistText = Command.getCommandLanguage(WhiteListCommand::class.java, locale).title
            mainContainer.add(
                    DashboardTitle(whitelistText),
                    DashboardText(getString(Category.CONFIGURATION, "whitelist_state0_description")),
                    generateChannelWhitelistField()
            )
        }

        if (anyCommandsAreAccessible(CommandPermissionsCommand::class)) {
            val commandPermissionsText = Command.getCommandLanguage(CommandPermissionsCommand::class.java, locale).title
            mainContainer.add(
                    DashboardTitle(commandPermissionsText),
                    DashboardText(getString(Category.CONFIGURATION, "cperms_message0").replace("**", "")),
                    generateCommandPermissionsField(guild)
            )
        }
    }

    private fun generateCommandPermissionsField(guild: Guild): DashboardComponent {
        val container = VerticalContainer(DashboardText(getString(Category.CONFIGURATION, "cperms_message1")))
        container.isCard = true

        val button = DashboardButton(getString(Category.CONFIGURATION, "cperms_button")) {
            if (!anyCommandsAreAccessible(CommandPermissionsCommand::class)) {
                return@DashboardButton ActionResult()
                        .withRedraw()
            }

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.COMMAND_PERMISSIONS_TRANSFER, atomicMember)

            val actionResult = ActionResult()
            if (CommandPermissions.transferCommandPermissions(guild, guildEntity)) {
                actionResult.withSuccessMessage(getString(Category.CONFIGURATION, "cperms_success"))
            } else {
                actionResult.withErrorMessage(getString(Category.CONFIGURATION, "cperms_failed"))
            }

            entityManager.transaction.commit()
            return@DashboardButton actionResult
        }
        button.style = DashboardButton.Style.PRIMARY
        val buttonField = HorizontalContainer()
        buttonField.add(button, HorizontalPusher())
        container.add(buttonField)

        return container
    }

    private fun generateChannelWhitelistField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val obsoleteWarning = DashboardText(getString(Category.CONFIGURATION, "cperms_obsolete_dashboard"))
        obsoleteWarning.style = DashboardText.Style.ERROR
        container.add(obsoleteWarning)

        container.add(
                DashboardMultiChannelsComboBox(
                        this,
                        "",
                        DashboardComboBox.DataType.GUILD_CHANNELS,
                        { it.whitelistedChannelIds },
                        true,
                        WhiteListCommand.MAX_CHANNELS,
                        WhiteListCommand::class,
                        BotLogEntity.Event.CHANNEL_WHITELIST
                )
        )

        return container
    }

    private fun generateCommandManagementField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val commandCategoryValues = Category.independentValues().map { DiscordEntity(it.id, getString(TextManager.COMMANDS, it.id)) }
        container.add(generateBlacklistComboBox(getString(Category.CONFIGURATION, "cman_state0_mcategories"), commandCategoryValues))

        val commandValues = CommandContainer.getFullCommandList()
                .map {
                    val trigger = Command.getCommandProperties(it).trigger
                    DiscordEntity(trigger, trigger)
                }
                .sortedBy { it.id }
        container.add(generateBlacklistComboBox(getString(Category.CONFIGURATION, "cman_state0_mcommands"), commandValues))
        return container
    }

    private fun generateBlacklistComboBox(label: String, values: List<DiscordEntity>
    ): DashboardComponent {
        val comboBox = DashboardComboBox(label, values, true, Int.MAX_VALUE) {
            if (!anyCommandsAreAccessible(CommandManagementCommand::class)) {
                return@DashboardComboBox ActionResult()
                        .withRedraw()
            }

            if (it.type == "add") {
                entityManager.transaction.begin()
                guildEntity.disabledCommandsAndCategories += it.data
                BotLogEntity.log(entityManager, BotLogEntity.Event.COMMAND_MANAGEMENT, atomicMember, it.data, null)
                entityManager.transaction.commit()
            } else if (it.type == "remove") {
                entityManager.transaction.begin()
                guildEntity.disabledCommandsAndCategories -= it.data
                BotLogEntity.log(entityManager, BotLogEntity.Event.COMMAND_MANAGEMENT, atomicMember, null, it.data)
                entityManager.transaction.commit()
            }
            ActionResult()
        }
        comboBox.selectedValues = values.filter { guildEntity.disabledCommandsAndCategories.contains(it.id) }
        return comboBox
    }

}