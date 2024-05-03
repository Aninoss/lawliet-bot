package dashboard.pages

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.CommandManager
import commands.listeners.OnAlertListener
import commands.runnables.configurationcategory.AlertsCommand
import core.CustomObservableMap
import core.TextManager
import core.atomicassets.AtomicGuildMessageChannel
import core.utils.BotPermissionUtil
import core.utils.JDAUtil
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
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.tracker.DBTracker
import mysql.modules.tracker.TrackerData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.time.Instant
import java.time.LocalDate
import java.util.*

@DashboardProperties(
        id = "alerts",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [AlertsCommand::class]
)
class AlertsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var command: Command? = null
    var channelId: Long? = null
    var commandKey = ""
    var userMessage = ""
    var minInterval = 0

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(AlertsCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val alertMap = DBTracker.getInstance().retrieve(guild.idLong)
        if (alertMap.isNotEmpty()) {
            val innerContainer = VerticalContainer(
                    DashboardText(getString(Category.CONFIGURATION, "alerts_dashboard_desc")),
                    generateAlertGrid(guild, alertMap)
            )
            innerContainer.isCard = true
            mainContainer.add(innerContainer)
        }
        mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, "alerts_state5_title")),
                generateNewAlertField(guild, alertMap)
        )
    }

    fun generateAlertGrid(guild: Guild, alertMap: CustomObservableMap<Int, TrackerData>): DashboardComponent {
        val rows = alertMap.values
                .filter { it.guildMessageChannel.isPresent }
                .sortedWith { a0, a1 ->
                    val channelO: Long = a0.guildMessageChannelId
                    val channel1: Long = a1.guildMessageChannelId
                    if (channelO == channel1) {
                        a0.creationTime.compareTo(a1.creationTime)
                    } else {
                        channelO.compareTo(channel1)
                    }
                }
                .map {
                    val atomicChannel = AtomicGuildMessageChannel(guild.idLong, it.guildMessageChannelId)
                    val values = arrayOf(it.commandTrigger, atomicChannel.getPrefixedName(locale), it.commandKey)
                    GridRow(it.hashCode().toString(), values)
                }

        val headers = getString(Category.CONFIGURATION, "alerts_dashboard_gridheaders").split('\n').toTypedArray()
        val grid = DashboardGrid(headers, rows) {
            val alertSlot = alertMap.get(it.data.toInt())
            if (alertSlot != null) {
                entityManager.transaction.begin()
                BotLogEntity.log(entityManager, BotLogEntity.Event.ALERTS, atomicMember, null, alertSlot.commandTrigger)
                entityManager.transaction.commit()
                alertSlot.delete()
            }
            ActionResult()
                    .withRedraw()
        }
        grid.rowButton = getString(Category.CONFIGURATION, "alerts_dashboard_gridremove")
        grid.enableConfirmationMessage(getString(Category.CONFIGURATION, "alerts_dashboard_gridconfirm"))

        return grid
    }

    fun generateNewAlertField(guild: Guild, alertMap: CustomObservableMap<Int, TrackerData>): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true
        container.add(generateCommandPropertiesField())

        val attachmentField = DashboardMultiLineTextField(getString(Category.CONFIGURATION, "alerts_dashboard_attachment"), 0, 1000) {
            if (isPremium) {
                userMessage = it.data
            }
            ActionResult()
        }
        attachmentField.value = userMessage
        attachmentField.isEnabled = isPremium
        attachmentField.editButton = false
        container.add(DashboardSeparator(), attachmentField)
        container.add(DashboardText(getString(Category.CONFIGURATION, "alerts_dashboard_attachment_help")))

        val minIntervalField = DashboardDurationField(getString(Category.CONFIGURATION, "alerts_dashboard_mininterval")) {
            if (isPremium) {
                minInterval = it.data.toInt()
            }
            ActionResult()
        }
        minIntervalField.value = minInterval.toLong()
        minIntervalField.isEnabled = isPremium
        minIntervalField.editButton = false
        container.add(DashboardSeparator(), DashboardText(getString(Category.CONFIGURATION, "alerts_dashboard_mininterval")))
        container.add(minIntervalField)
        container.add(DashboardText(getString(Category.CONFIGURATION, "alerts_dashboard_mininterval_help")))

        val buttonField = HorizontalContainer()
        val addButton = DashboardButton(getString(Category.CONFIGURATION, "alerts_dashboard_add")) {
            val premium = isPremium
            if (alertMap.values.size >= AlertsCommand.LIMIT_SERVER && !premium) { /* server alert limit */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "alerts_toomuch_server", AlertsCommand.LIMIT_SERVER.toString()))
            }

            val channel = channelId?.let { guild.getChannelById(GuildMessageChannel::class.java, it.toString()) }
            if (channel == null) { /* invalid channel */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "alerts_invalidchannel"))
            }
            if (!BotPermissionUtil.canWriteEmbed(channel)) { /* no permissions in channel */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(TextManager.GENERAL, "permission_channel", "#${channel.getName()}"))
            }
            if (alertMap.values.filter { it.guildMessageChannelId == channelId }.size >= AlertsCommand.LIMIT_CHANNEL && !premium) { /* channel alert limit */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "alerts_toomuch_channel", AlertsCommand.LIMIT_CHANNEL.toString()))
            }

            if (command == null) { /* invalid command */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "alerts_invalidcommand"))
            }
            if (command!!.commandProperties.patreonRequired && !premium) { /* command requires premium */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(TextManager.GENERAL, "patreon_unlock"))
            }
            if (command!!.commandProperties.nsfw && !JDAUtil.channelIsNsfw(channel)) { /* command requires nsfw */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(TextManager.GENERAL, "nsfw_block_description", prefix).replace("`", "\""))
            }

            val commandUsesKey = (command as OnAlertListener).trackerUsesKey()
            if (!commandUsesKey) {
                commandKey = ""
            } else if (commandKey.isEmpty()) { /* no argument specified */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "alerts_dashboard_specifykey"))
            }

            if (!BotPermissionUtil.memberCanMentionRoles(channel, atomicMember.get().get(), userMessage)) { /* custom text invalid mentions */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(TextManager.GENERAL, "user_nomention"))
            }

            val alreadyExists = alertMap.values.any {
                it.commandTrigger == command?.trigger &&
                        it.guildMessageChannelId == channelId &&
                        it.commandKey == commandKey
            }
            if (alreadyExists) { /* alert already exists */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "alerts_state1_alreadytracking", command!!.trigger))
            }

            val trackerData = TrackerData(
                    guild.idLong,
                    channelId!!,
                    command!!.trigger,
                    null,
                    commandKey,
                    Instant.now(),
                    null,
                    null,
                    userMessage,
                    Instant.now(),
                    minInterval
            )
            clearAttributes()

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.ALERTS, atomicMember, trackerData.commandTrigger, null)
            entityManager.transaction.commit()

            alertMap.put(trackerData.hashCode(), trackerData)
            AlertScheduler.loadAlert(trackerData)
            ActionResult()
                    .withRedraw()
        }
        addButton.style = DashboardButton.Style.PRIMARY
        buttonField.add(addButton, HorizontalPusher())
        container.add(DashboardSeparator(), buttonField)
        return container
    }

    fun generateCommandPropertiesField(): DashboardComponent {
        val container = HorizontalContainer()
        container.allowWrap = true

        val channelLabel = getString(Category.CONFIGURATION, "alerts_dashboard_channel")
        val channelComboBox = DashboardComboBox(channelLabel, DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS, false, 1) {
            channelId = it.data.toLong()
            ActionResult()
        }
        if (channelId != null) {
            val atomicChannel = AtomicGuildMessageChannel(atomicGuild.idLong, channelId!!)
            channelComboBox.selectedValues = listOf(DiscordEntity(channelId.toString(), atomicChannel.getPrefixedName(locale)))
        }
        container.add(channelComboBox)

        val commandLabel = getString(Category.CONFIGURATION, "alerts_dashboard_command")
        val commandValues = CommandContainer.getTrackerCommands()
                .map {
                    val command = CommandManager.createCommandByClass(it as Class<Command>, locale, prefix)
                    extractCommand(command)
                }
                .sortedBy { it.id }
        val commandComboBox = DashboardComboBox(commandLabel, commandValues, false, 1) {
            command = CommandManager.createCommandByTrigger(it.data, locale, prefix).get()
            commandKey = ""
            ActionResult()
                    .withRedraw()
        }
        if (command != null) {
            commandComboBox.selectedValues = listOf(extractCommand(command!!))
        }
        container.add(commandComboBox)

        val withKey = command != null && (command as OnAlertListener).trackerUsesKey()
        val argsLabel = if (withKey) {
            getString(command!!.category, command!!.trigger + "_trackerkey_short")
        } else {
            getString(Category.CONFIGURATION, "alerts_dashboard_arg")
        }

        val argsTextField = DashboardTextField(argsLabel, 0, AlertsCommand.LIMIT_KEY_LENGTH) {
            commandKey = it.data
            ActionResult()
        }
        argsTextField.value = commandKey
        argsTextField.editButton = false
        argsTextField.isEnabled = withKey

        container.add(argsTextField)

        return container
    }

    private fun extractCommand(command: Command): DiscordEntity {
        val trigger = command.trigger
        val index = if (command.commandProperties.patreonRequired) {
            1
        } else if (command.getReleaseDate().orElse(LocalDate.now()).isAfter(LocalDate.now())) {
            2
        } else {
            0
        }

        return DiscordEntity(trigger, getString(Category.CONFIGURATION, "alerts_dashboard_trigger", index, trigger))
    }

    fun clearAttributes() {
        command = null
        channelId = null
        commandKey = ""
        userMessage = ""
        minInterval = 0
    }

}