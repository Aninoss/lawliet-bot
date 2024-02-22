package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.TicketCommand
import core.TextManager
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import modules.Ticket
import mysql.hibernate.entity.guild.GuildEntity
import mysql.hibernate.entity.guild.TicketsEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "ticket",
        userPermissions = [Permission.MANAGE_CHANNEL],
        botPermissions = [Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES],
        commandAccessRequirements = [TicketCommand::class]
)
class TicketCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var createMessageChannelId: Long? = null

    val ticketsEntity: TicketsEntity
        get() = guildEntity.tickets

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(TicketCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.add(
                generateTicketMessageField(),
                generateGeneralField(),
                generateTicketCreateOptionsField(),
                generateTicketCloseOptionsField(),
                generateAutoCloseOnInactivityField()
        )
    }

    private fun generateTicketMessageField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true
        container.putCssProperties("margin-bottom", "1rem")

        val innerContainer = HorizontalContainer()
        innerContainer.alignment = HorizontalContainer.Alignment.CENTER
        innerContainer.allowWrap = true

        val channelComboBox = DashboardTextChannelComboBox(
                "",
                locale,
                atomicGuild.idLong,
                createMessageChannelId,
                false
        ) {
            createMessageChannelId = it.data.toLong()
            ActionResult()
                    .withRedraw()
        }
        channelComboBox.putCssProperties("margin-top", "0.5em")

        val sendButton = DashboardButton(getString(category = Category.CONFIGURATION, "ticket_state4_title")) {
            val error = Ticket.sendTicketMessage(locale, atomicGuild.get().get().getTextChannelById(createMessageChannelId!!))
            if (error == null) {
                return@DashboardButton ActionResult()
                        .withSuccessMessage(getString(Category.CONFIGURATION, "ticket_message_sent"))
            } else {
                return@DashboardButton ActionResult()
                        .withErrorMessage(error)
            }
        }
        sendButton.isEnabled = createMessageChannelId != null
        sendButton.style = DashboardButton.Style.PRIMARY
        sendButton.setCanExpand(false)

        innerContainer.add(
                channelComboBox,
                sendButton
        )
        container.add(innerContainer)
        return container
    }

    private fun generateGeneralField(): DashboardComponent {
        val container = VerticalContainer()

        val logChannel = DashboardTextChannelComboBox(
                getString(Category.CONFIGURATION, "ticket_state0_mannouncement"),
                locale,
                atomicGuild.idLong,
                ticketsEntity.logChannelId,
                true
        ) { e ->
            val channel = atomicGuild.get().map { it.getTextChannelById(e.data.toLong()) }
                    .orElse(null)
            if (channel == null) {
                return@DashboardTextChannelComboBox ActionResult()
                        .withRedraw()
            }

            val channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(locale, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
            if (channelMissingPerms != null) {
                return@DashboardTextChannelComboBox ActionResult()
                        .withRedraw()
                        .withErrorMessage(channelMissingPerms)
            }

            ticketsEntity.beginTransaction()
            ticketsEntity.logChannelId = channel.idLong
            ticketsEntity.commitTransaction()
            return@DashboardTextChannelComboBox ActionResult()
        }
        container.add(logChannel)

        val staffContainer = HorizontalContainer()
        staffContainer.allowWrap = true

        val staffRoles = DashboardMultiRolesComboBox(
                this,
                getString(Category.CONFIGURATION, "ticket_state0_mstaffroles"),
                { it.tickets.staffRoleIds },
                true,
                TicketCommand.MAX_STAFF_ROLES,
                false
        )
        staffContainer.add(staffRoles)

        val assignmentValues = TicketsEntity.AssignmentMode.values().mapIndexed() { i, mode ->
            DiscordEntity(i.toString(), getString(Category.CONFIGURATION, "ticket_assignment_modes").split("\n")[i])
        }
        val assignmentMode = DashboardComboBox(
                getString(Category.CONFIGURATION, "ticket_state0_massign"),
                assignmentValues,
                false,
                1
        ) {
            ticketsEntity.beginTransaction()
            ticketsEntity.assignmentMode = TicketsEntity.AssignmentMode.values()[it.data.toInt()]
            ticketsEntity.commitTransaction()
            return@DashboardComboBox ActionResult()
        }
        assignmentMode.selectedValues = listOf(assignmentValues[ticketsEntity.assignmentMode.ordinal])
        staffContainer.add(assignmentMode)
        container.add(staffContainer)



        return container
    }

    private fun generateAutoCloseOnInactivityField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.CONFIGURATION, "ticket_state0_mcloseoninactivity")))

        val active = DashboardSwitch(getString(Category.CONFIGURATION, "ticket_dashboard_active")) {
            ticketsEntity.beginTransaction()
            if (it.data) {
                ticketsEntity.autoCloseHours = 1
            } else {
                ticketsEntity.autoCloseHours = null
            }
            ticketsEntity.commitTransaction()
            return@DashboardSwitch ActionResult()
                    .withRedraw()
        }
        active.isChecked = ticketsEntity.autoCloseHoursEffectively != null
        active.isEnabled = isPremium
        active.enableConfirmationMessage(getString(Category.CONFIGURATION, "ticket_dashboard_autoclose_warning"))
        container.add(active, DashboardSeparator())

        val duration = DashboardDurationField(getString(Category.CONFIGURATION, "ticket_dashboard_autoclose_duration")) {
            ticketsEntity.beginTransaction()
            ticketsEntity.autoCloseHours = it.data.toInt() / 60
            ticketsEntity.commitTransaction()

            return@DashboardDurationField ActionResult()
                    .withRedraw()
        }
        duration.includeMinutes = false
        duration.enableConfirmationMessage(getString(Category.CONFIGURATION, "ticket_dashboard_autoclose_warning"))
        if (ticketsEntity.autoCloseHoursEffectively != null) {
            duration.value = ticketsEntity.autoCloseHoursEffectively!!.toLong() * 60
        }
        duration.isEnabled = isPremium
        container.add(duration)

        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }

        return container
    }

    private fun generateTicketCreateOptionsField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.CONFIGURATION, "ticket_state0_mcreateoptions")))

        val pingStaff = DashboardSwitch(getString(Category.CONFIGURATION, "ticket_state0_mping")) {
            ticketsEntity.beginTransaction()
            ticketsEntity.pingStaffRoles = it.data
            ticketsEntity.commitTransaction()

            return@DashboardSwitch ActionResult()
        }
        pingStaff.isChecked = ticketsEntity.pingStaffRoles
        container.add(pingStaff, DashboardSeparator())

        val enforceTextInputs = DashboardSwitch(getString(Category.CONFIGURATION, "ticket_state0_mtextinput")) {
            ticketsEntity.beginTransaction()
            ticketsEntity.enforceModal = it.data
            ticketsEntity.commitTransaction()

            return@DashboardSwitch ActionResult()
        }
        enforceTextInputs.isChecked = ticketsEntity.enforceModal
        container.add(enforceTextInputs, DashboardSeparator())

        val greetingText = DashboardMultiLineTextField(
                getString(Category.CONFIGURATION, "ticket_state0_mcreatemessage"),
                0,
                TicketCommand.MAX_GREETING_TEXT_LENGTH
        ) {
            ticketsEntity.beginTransaction()
            ticketsEntity.greetingText = it.data.ifEmpty { null }
            ticketsEntity.commitTransaction()

            return@DashboardMultiLineTextField ActionResult()
        }
        greetingText.value = ticketsEntity.greetingText ?: ""
        container.add(greetingText)

        return container
    }

    private fun generateTicketCloseOptionsField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.CONFIGURATION, "ticket_state0_mcloseoptions")))

        val membersCanCloseTickets = DashboardSwitch(getString(Category.CONFIGURATION, "ticket_state0_mmembercanclose")) {
            ticketsEntity.beginTransaction()
            ticketsEntity.membersCanCloseTickets = it.data
            ticketsEntity.commitTransaction()

            return@DashboardSwitch ActionResult()
        }
        membersCanCloseTickets.isChecked = ticketsEntity.membersCanCloseTickets
        container.add(membersCanCloseTickets, DashboardSeparator())

        val saveProtocols = DashboardSwitch(getString(Category.CONFIGURATION, "ticket_dashboard_protocols")) {
            ticketsEntity.beginTransaction()
            ticketsEntity.protocols = it.data
            ticketsEntity.commitTransaction()

            return@DashboardSwitch ActionResult()
        }
        saveProtocols.isChecked = ticketsEntity.protocolsEffectively
        saveProtocols.isEnabled = isPremium
        container.add(saveProtocols, DashboardSeparator())

        val deleteChannels = DashboardSwitch(getString(Category.CONFIGURATION, "ticket_state0_mdeletechannel")) {
            ticketsEntity.beginTransaction()
            ticketsEntity.deleteChannelsOnClose = it.data
            ticketsEntity.commitTransaction()

            return@DashboardSwitch ActionResult()
        }
        deleteChannels.isChecked = ticketsEntity.deleteChannelsOnClose
        container.add(deleteChannels)

        return container
    }

}