package dashboard.pages

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.configurationcategory.CommandChannelShortcutsCommand
import core.TextManager
import core.atomicassets.AtomicGuildMessageChannel
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardButton
import dashboard.component.DashboardComboBox
import dashboard.component.DashboardText
import dashboard.component.DashboardTitle
import dashboard.components.DashboardChannelComboBox
import dashboard.container.DashboardListContainer
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.*

@DashboardProperties(
        id = "ccshortcuts",
        userPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [CommandChannelShortcutsCommand::class]
)
class CommandChannelShortcutsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var trigger: String? = null
    var channelId: Long? = null

    val commandChannelShortcuts
        get() = guildEntity.commandChannelShortcuts

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(CommandChannelShortcutsCommand::class.java, locale).title
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "ccshortcuts_default_desc")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"), DashboardText.Style.ERROR)
            mainContainer.add(text)
            return
        }

        if (commandChannelShortcuts.isNotEmpty()) {
            mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, "ccshortcuts_dashboard_active")),
                generateShortcutListContainer(guild)
            )
        }

        mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, "ccshortcuts_add_title")),
                generateNewShortcutField(guild)
        )
    }

    fun generateShortcutListContainer(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val items = commandChannelShortcuts.entries
                .map { shortcut ->
                    val button = DashboardButton(getString(Category.CONFIGURATION, "ccshortcuts_dashboard_remove")) {
                        guildEntity.beginTransaction()
                        commandChannelShortcuts.remove(shortcut.key)
                        BotLogEntity.log(entityManager, BotLogEntity.Event.COMMAND_CHANNEL_SHORTCUTS_DELETE, atomicMember, shortcut.key)
                        guildEntity.commitTransaction()

                        return@DashboardButton ActionResult()
                                .withRedraw()
                    }
                    button.style = DashboardButton.Style.DANGER

                    val atomicChannel = AtomicGuildMessageChannel(guild.idLong, shortcut.key)
                    val itemContainer = HorizontalContainer(
                            DashboardText("${atomicChannel.getPrefixedName(locale)}: ${shortcut.value}"),
                            HorizontalPusher(),
                            button
                    )
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    return@map itemContainer
                }

        val listContainer = DashboardListContainer()
        listContainer.add(items)
        return listContainer
    }

    fun generateNewShortcutField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val horizontalContainerer = HorizontalContainer()
        horizontalContainerer.alignment = HorizontalContainer.Alignment.BOTTOM
        horizontalContainerer.allowWrap = true

        val channelLabel = getString(Category.CONFIGURATION, "ccshortcuts_add_channel")
        val channelComboBox = DashboardChannelComboBox(
                this,
                channelLabel,
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                null,
                false,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
        ) {
            channelId = it.data.toLong()
            ActionResult()
                    .withRedraw()
        }
        if (channelId != null) {
            val atomicChannel = AtomicGuildMessageChannel(atomicGuild.idLong, channelId!!)
            channelComboBox.selectedValues = listOf(DiscordEntity(channelId.toString(), atomicChannel.getPrefixedName(locale)))
        }
        horizontalContainerer.add(channelComboBox)

        val commandLabel = getString(Category.CONFIGURATION, "ccshortcuts_add_command")
        val commandValues = CommandContainer.getFullCommandList()
                .map {
                    val trigger = Command.getCommandProperties(it).trigger
                    DiscordEntity(trigger, trigger)
                }
                .sortedBy { it.id }
        val commandComboBox = DashboardComboBox(commandLabel, commandValues, false, 1) {
            trigger = it.data
            ActionResult()
                    .withRedraw()
        }
        if (trigger != null) {
            commandComboBox.selectedValues = listOf(DiscordEntity(trigger!!, trigger!!))
        }
        horizontalContainerer.add(commandComboBox)

        val addButton = DashboardButton(getString(Category.CONFIGURATION, "ccshortcuts_dashboard_add")) {
            if (channelId == null || trigger == null) {
                return@DashboardButton ActionResult()
                        .withRedraw()
            }

            val channel = channelId?.let { guild.getChannelById(GuildMessageChannel::class.java, it.toString()) }
            if (channel == null) { /* invalid channel */
                return@DashboardButton ActionResult()
                        .withRedraw()
            }
            if (!BotPermissionUtil.canWrite(channel)) { /* no permissions in channel */
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(TextManager.GENERAL, "permission_channel_send", "#${channel.getName()}"))
            }
            if (commandChannelShortcuts.containsKey(channelId)) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "ccshortcuts_log_channel_exist"))
            }

            guildEntity.beginTransaction()
            commandChannelShortcuts.put(channelId!!, trigger!!)
            BotLogEntity.log(entityManager, BotLogEntity.Event.COMMAND_CHANNEL_SHORTCUTS_ADD, atomicMember, channelId!!)
            guildEntity.commitTransaction()

            channelId = null
            trigger = null

            ActionResult()
                    .withRedraw()
        }
        addButton.isEnabled = trigger != null && channelId != null
        addButton.style = DashboardButton.Style.PRIMARY

        horizontalContainerer.add(addButton)
        container.add(horizontalContainerer)
        return container
    }

}