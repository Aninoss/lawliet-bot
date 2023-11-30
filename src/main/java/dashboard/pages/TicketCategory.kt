package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.TicketCommand
import core.TextManager
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
import mysql.modules.ticket.DBTicket
import mysql.modules.ticket.TicketData
import mysql.modules.ticket.TicketData.TicketAssignmentMode
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

    var createMessageChannelId: Long? = null;

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(TicketCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val ticketData = DBTicket.getInstance().retrieve(guild.idLong)

        mainContainer.add(
                generateTicketMessageField(),
                generateGeneralField(ticketData),
                generateTicketCreateOptionsField(ticketData),
                generateTicketCloseOptionsField(ticketData),
                generateAutoCloseOnInactivityField(ticketData)
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

        val sendButton = DashboardButton(getString(category = Category.UTILITY, "ticket_state4_title")) {
            val error = Ticket.sendTicketMessage(locale, atomicGuild.get().get().getTextChannelById(createMessageChannelId!!))
            if (error == null) {
                return@DashboardButton ActionResult()
                        .withSuccessMessage(getString(Category.UTILITY, "ticket_message_sent"))
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

    private fun generateGeneralField(ticketData: TicketData): DashboardComponent {
        val container = VerticalContainer()

        val logChannel = DashboardTextChannelComboBox(
                getString(Category.UTILITY, "ticket_state0_mannouncement"),
                locale,
                atomicGuild.idLong,
                ticketData.announcementTextChannelId.orElse(null),
                true
        ) {
            DBTicket.getInstance().retrieve(atomicGuild.idLong)
                    .setAnnouncementTextChannelId(it.data?.toLong())
            return@DashboardTextChannelComboBox ActionResult()
        }
        container.add(logChannel)

        val staffContainer = HorizontalContainer()
        staffContainer.allowWrap = true

        val staffRoles = DashboardMultiRolesComboBox(
                this,
                getString(Category.UTILITY, "ticket_state0_mstaffroles"),
                { ticketData.staffRoleIds },
                true,
                TicketCommand.MAX_STAFF_ROLES,
                false
        )
        staffContainer.add(staffRoles)

        val assignmentValues = TicketAssignmentMode.values().mapIndexed() { i, mode ->
            DiscordEntity(i.toString(), getString(Category.UTILITY, "ticket_assignment_modes").split("\n")[i])
        }
        val assignmentMode = DashboardComboBox(
                getString(Category.UTILITY, "ticket_state0_massign"),
                assignmentValues,
                false,
                1
        ) {
            DBTicket.getInstance().retrieve(atomicGuild.idLong).ticketAssignmentMode = TicketAssignmentMode.values()[it.data.toInt()]
            return@DashboardComboBox ActionResult()
        }
        assignmentMode.selectedValues = listOf(assignmentValues[ticketData.ticketAssignmentMode.ordinal])
        staffContainer.add(assignmentMode)
        container.add(staffContainer)



        return container
    }

    private fun generateAutoCloseOnInactivityField(ticketData: TicketData): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.UTILITY, "ticket_state0_mcloseoninactivity")))

        val active = DashboardSwitch(getString(Category.UTILITY, "ticket_dashboard_active")) {
            if (it.data) {
                DBTicket.getInstance().retrieve(atomicGuild.idLong).autoCloseHours = 1
            } else {
                DBTicket.getInstance().retrieve(atomicGuild.idLong).autoCloseHours = null
            }
            return@DashboardSwitch ActionResult()
                    .withRedraw()
        }
        active.isChecked = ticketData.autoCloseHoursEffectively != null
        active.isEnabled = isPremium
        active.enableConfirmationMessage(getString(Category.UTILITY, "ticket_dashboard_autoclose_warning"))
        container.add(active, DashboardSeparator())

        val duration = DashboardDurationField(getString(Category.UTILITY, "ticket_dashboard_autoclose_duration")) {
            DBTicket.getInstance().retrieve(atomicGuild.idLong).autoCloseHours = it.data.toInt() / 60
            return@DashboardDurationField ActionResult()
                    .withRedraw()
        }
        duration.includeMinutes = false
        duration.enableConfirmationMessage(getString(Category.UTILITY, "ticket_dashboard_autoclose_warning"))
        if (ticketData.autoCloseHoursEffectively != null) {
            duration.value = ticketData.autoCloseHoursEffectively.toLong() * 60
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

    private fun generateTicketCreateOptionsField(ticketData: TicketData): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.UTILITY, "ticket_state0_mcreateoptions")))

        val pingStaff = DashboardSwitch(getString(Category.UTILITY, "ticket_state0_mping")) {
            DBTicket.getInstance().retrieve(atomicGuild.idLong).pingStaff = it.data
            return@DashboardSwitch ActionResult()
        }
        pingStaff.isChecked = ticketData.pingStaff
        container.add(pingStaff, DashboardSeparator())

        val enforceTextInputs = DashboardSwitch(getString(Category.UTILITY, "ticket_state0_mtextinput")) {
            DBTicket.getInstance().retrieve(atomicGuild.idLong).userMessages = it.data
            return@DashboardSwitch ActionResult()
        }
        enforceTextInputs.isChecked = ticketData.userMessages
        container.add(enforceTextInputs, DashboardSeparator())

        val greetingText = DashboardMultiLineTextField(
                getString(Category.UTILITY, "ticket_state0_mcreatemessage"),
                0,
                TicketCommand.MAX_GREETING_TEXT_LENGTH
        ) {
            if (it.data.isEmpty()) {
                DBTicket.getInstance().retrieve(atomicGuild.idLong).setCreateMessage(null)
            } else {
                DBTicket.getInstance().retrieve(atomicGuild.idLong).setCreateMessage(it.data)
            }
            return@DashboardMultiLineTextField ActionResult()
        }
        greetingText.value = ticketData.createMessage.orElse("")
        container.add(greetingText)

        return container
    }

    private fun generateTicketCloseOptionsField(ticketData: TicketData): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.UTILITY, "ticket_state0_mcloseoptions")))

        val membersCanCloseTickets = DashboardSwitch(getString(Category.UTILITY, "ticket_state0_mmembercanclose")) {
            DBTicket.getInstance().retrieve(atomicGuild.idLong).setMemberCanClose(it.data)
            return@DashboardSwitch ActionResult()
        }
        membersCanCloseTickets.isChecked = ticketData.memberCanClose()
        container.add(membersCanCloseTickets, DashboardSeparator())

        val saveProtocols = DashboardSwitch(getString(Category.UTILITY, "ticket_dashboard_protocols")) {
            DBTicket.getInstance().retrieve(atomicGuild.idLong).protocol = it.data
            return@DashboardSwitch ActionResult()
        }
        saveProtocols.isChecked = ticketData.protocolEffectively
        saveProtocols.isEnabled = isPremium
        container.add(saveProtocols, DashboardSeparator())

        val deleteChannels = DashboardSwitch(getString(Category.UTILITY, "ticket_state0_mdeletechannel")) {
            DBTicket.getInstance().retrieve(atomicGuild.idLong).deleteChannelOnTicketClose = it.data
            return@DashboardSwitch ActionResult()
        }
        deleteChannels.isChecked = ticketData.deleteChannelOnTicketClose
        container.add(deleteChannels)

        return container
    }

}