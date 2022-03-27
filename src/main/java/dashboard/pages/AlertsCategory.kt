package dashboard.pages

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.CommandManager
import commands.listeners.OnAlertListener
import commands.runnables.utilitycategory.AlertsCommand
import core.CustomObservableMap
import core.TextManager
import core.atomicassets.AtomicBaseGuildMessageChannel
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import dashboard.data.GridRow
import modules.schedulers.AlertScheduler
import mysql.modules.tracker.DBTracker
import mysql.modules.tracker.TrackerData
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel
import net.dv8tion.jda.api.entities.Guild
import java.time.Instant
import java.util.*

@DashboardProperties(
    id = "alerts"
)
class AlertsCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    var commandTrigger: String? = null
    var channelId: Long? = null
    var commandKey = ""
    var userMessage: String = ""

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(AlertsCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val alertMap = DBTracker.getInstance().retrieve(guild.idLong)
        mainContainer.add(
            DashboardText(getString(Category.UTILITY, "alerts_dashboard_desc")),
            generateAlertGrid(guild, alertMap),
            generateNewAlertField(guild, alertMap)
        )
    }

    fun generateAlertGrid(guild: Guild, alertMap: CustomObservableMap<Int, TrackerData>): DashboardComponent {
        val rows = alertMap.values
            .filter { it.baseGuildMessageChannel.isPresent }
            .sortedWith { a0, a1 ->
                val channelO: Long = a0.baseGuildMessageChannelId
                val channel1: Long = a1.baseGuildMessageChannelId
                if (channelO == channel1) {
                    a0.creationTime.compareTo(a1.creationTime)
                } else {
                    channelO.compareTo(channel1)
                }
            }
            .map {
                val atomicChannel = AtomicBaseGuildMessageChannel(guild.idLong, it.baseGuildMessageChannelId)
                val values = arrayOf(atomicChannel.prefixedName, it.commandTrigger, it.commandKey)
                GridRow(it.hashCode().toString(), values)
            }

        val headers = getString(Category.UTILITY, "alerts_dashboard_gridheaders").split('\n').toTypedArray()
        val grid = DashboardGrid(headers, rows) {
            alertMap.get(it.data.toInt())?.delete()
            ActionResult(true)
        }
        grid.rowButton = getString(Category.UTILITY, "alerts_dashboard_gridremove")
        grid.enableConfirmationMessage(getString(Category.UTILITY, "alerts_dashboard_gridconfirm"))

        return grid
    }

    fun generateNewAlertField(guild: Guild, alertMap: CustomObservableMap<Int, TrackerData>): DashboardComponent {
        val container = VerticalContainer()
        container.add(
            DashboardTitle(getString(Category.UTILITY, "alerts_state5_title")),
            generateCommandPropertiesField()
        )

        val attachmentField = DashboardMultiLineTextField(getString(Category.UTILITY, "alerts_dashboard_attachment"), 0, 1000) {
            userMessage = it.data
            ActionResult(false)
        }
        attachmentField.isEnabled = isPremium
        attachmentField.editButton = false
        container.add(DashboardSeparator(), attachmentField)
        container.add(DashboardText(getString(Category.UTILITY, "alerts_dashboard_attachment_help")))

        val buttonField = HorizontalContainer()
        val addButton = DashboardButton(getString(Category.UTILITY, "alerts_dashboard_add")) {
            val premium = isPremium
            if (alertMap.values.size >= AlertsCommand.LIMIT_SERVER && !premium) { /* server alert limit */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(Category.UTILITY, "alerts_toomuch_server", AlertsCommand.LIMIT_SERVER.toString()))
            }

            val channel = channelId ?.let { guild.getChannelById(BaseGuildMessageChannel::class.java, it.toString()) }
            if (channel == null) { /* invalid channel */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(Category.UTILITY, "alerts_invalidchannel"))
            }
            if (!BotPermissionUtil.canWriteEmbed(channel)) { /* no permissions in channel */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(TextManager.GENERAL, "permission_channel", "#${channel.getName()}"))
            }
            if (alertMap.values.filter { it.baseGuildMessageChannelId == channelId }.size >= AlertsCommand.LIMIT_CHANNEL && !premium) { /* channel alert limit */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(Category.UTILITY, "alerts_toomuch_channel", AlertsCommand.LIMIT_CHANNEL.toString()))
            }

            if (commandTrigger == null) { /* invalid command */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(Category.UTILITY, "alerts_invalidcommand"))
            }
            val command = CommandManager.createCommandByTrigger(commandTrigger, locale, prefix).get()
            if (command.commandProperties.patreonRequired && !premium) { /* command requires premium */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(TextManager.GENERAL, "patreon_unlock"))
            }
            if (command.commandProperties.nsfw && !channel.isNSFW) { /* command requires nsfw */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(TextManager.GENERAL, "nsfw_block_description"))
            }

            val commandUsesKey = (command as OnAlertListener).trackerUsesKey()
            if (!commandUsesKey) {
                commandKey = ""
            } else if (commandKey.isEmpty()) { /* no argument specified */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(Category.UTILITY, "alerts_dashboard_specifykey"))
            }

            if (!BotPermissionUtil.memberCanMentionRoles(channel, atomicMember.get().get(), userMessage)) { /* custom text invalid mentions */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(TextManager.GENERAL, "user_nomention"))
            }

            val alreadyExists = alertMap.values.any {
                it.commandTrigger == commandTrigger &&
                        it.baseGuildMessageChannelId == channelId &&
                        it.commandKey == commandKey
            }
            if (alreadyExists) { /* alert already exists */
                return@DashboardButton ActionResult(false)
                    .withErrorMessage(getString(Category.UTILITY, "alerts_state1_alreadytracking", commandTrigger!!))
            }

            val trackerData = TrackerData(
                guild.idLong,
                channelId!!,
                commandTrigger!!,
                null,
                commandKey,
                Instant.now(),
                null,
                null,
                userMessage,
                Instant.now()
            )
            alertMap.put(trackerData.hashCode(), trackerData)
            AlertScheduler.loadAlert(trackerData)
            ActionResult(true)
        }
        addButton.style = DashboardButton.Style.PRIMARY
        buttonField.add(addButton, HorizontalPusher())
        container.add(DashboardSeparator(), buttonField)
        return container
    }

    fun generateCommandPropertiesField(): DashboardComponent {
        val container = HorizontalContainer()
        container.allowWrap = true

        val channelLabel = getString(Category.UTILITY, "alerts_dashboard_channel")
        val channelComboBox = DashboardComboBox(channelLabel, DashboardComboBox.DataType.BASE_GUILD_MESSAGE_CHANNELS, false, 1) {
            channelId = it.data.toLong()
            ActionResult(false)
        }
        container.add(channelComboBox)

        val commandLabel = getString(Category.UTILITY, "alerts_dashboard_command")
        val commandValues = CommandContainer.getTrackerCommands()
            .map {
                val trigger = Command.getCommandProperties(it as Class<Command>).trigger
                val premium = CommandManager.createCommandByClass(it, locale, prefix).commandProperties.patreonRequired
                DiscordEntity(trigger, getString(Category.UTILITY, "alerts_dashboard_trigger", premium, trigger))
            }
            .sortedBy { it.id }
        val commandComboBox = DashboardComboBox(commandLabel, commandValues, false, 1) {
            commandTrigger = it.data
            ActionResult(false)
        }
        container.add(commandComboBox)

        val argsLabel = getString(Category.UTILITY, "alerts_dashboard_arg")
        val argsTextField = DashboardTextField(argsLabel, 0, AlertsCommand.LIMIT_KEY_LENGTH) {
            commandKey = it.data
            ActionResult(false)
        }
        argsTextField.editButton = false
        container.add(argsTextField)

        return container
    }

}