package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.ReactionRolesCommand
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicStandardGuildMessageChannel
import core.emojiconnection.EmojiConnection
import core.utils.MentionUtil
import core.utils.StringUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardRoleComboBox
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.GridRow
import modules.ReactionMessage
import modules.ReactionRoles
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import java.util.*

@DashboardProperties(
    id = "reactionroles",
    botPermissions = [Permission.MANAGE_ROLES, Permission.MESSAGE_HISTORY],
    userPermissions = [Permission.MANAGE_ROLES],
)
class ReactionRolesCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    var channelId: Long? = null
    var title = ""
    var desc = ""
    var roleRemovement = true
    var multipleRoles = true
    var image: String? = null
    var messageId: Long? = null
    var slots: ArrayList<Slot> = ArrayList()

    var newSlotEmoji: Emoji? = null
    var newSlotRole: Role? = null

    var editMode = false

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(ReactionRolesCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val reactionMessages: List<ReactionMessage> = ReactionRoles.getReactionMessagesInGuild(atomicGuild.idLong)

        if (!editMode) {
            mainContainer.add(
                generateReactionRolesTable(guild, reactionMessages)
            )
        }

        mainContainer.add(generateReactionRolesDataField(guild))
    }

    private fun generateReactionRolesTable(guild: Guild, reactionMessages: List<ReactionMessage>): DashboardComponent {
        val title = getString(Category.UTILITY, "reactionroles_dashboard_active_title")
        val rowButton = getString(Category.UTILITY, "reactionroles_dashboard_active_button")

        val container = VerticalContainer()
        container.add(DashboardTitle(title))

        val rows = reactionMessages
            .map {
                val atomicChannel =
                    AtomicStandardGuildMessageChannel(guild.idLong, it.standardGuildMessageChannelId)
                val values = arrayOf(it.title, atomicChannel.prefixedName)
                GridRow(it.messageId.toString(), values)
            }

        val headers = getString(Category.UTILITY, "reactionroles_dashboard_active_header").split('\n').toTypedArray()
        val grid = DashboardGrid(headers, rows) {
            val reactionRoleMessage = ReactionRoles.getReactionMessagesInGuild(atomicGuild.idLong)
                .filter { m -> m.messageId == it.data.toLong() }
                .firstOrNull()
            if (reactionRoleMessage != null) {
                readValuesFromReactionMessage(guild, reactionRoleMessage)
                switchMode(true)
            }
            ActionResult()
                .withRedrawScrollToTop()
        }
        grid.rowButton = rowButton
        container.add(grid)

        return container
    }

    private fun generateReactionRolesDataField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()

        val headerTitle = if (editMode) {
            getString(Category.UTILITY, "reactionroles_state2_title")
        } else {
            getString(Category.UTILITY, "reactionroles_state1_title")
        }
        container.add(DashboardTitle(headerTitle))

        val channelTitleContainer = HorizontalContainer()
        channelTitleContainer.allowWrap = true

        val channelLabel = getString(Category.UTILITY, "reactionroles_dashboard_channel")
        val channelComboBox = DashboardTextChannelComboBox(channelLabel, guild.idLong, channelId, false) {
            if (editMode) {
                return@DashboardTextChannelComboBox ActionResult()
            }

            channelId = it.data.toLong()
            ActionResult()
        }
        channelComboBox.isEnabled = !editMode
        channelTitleContainer.add(channelComboBox)

        val titleTextfield =
            DashboardTextField(getString(Category.UTILITY, "reactionroles_state3_mtitle"), 1, ReactionRolesCommand.TITLE_LENGTH_MAX) {
                this.title = it.data
                ActionResult()
            }
        if (!title.isEmpty()) {
            titleTextfield.value = this.title
        }
        titleTextfield.editButton = false
        titleTextfield.placeholder = getString(Category.UTILITY, "reactionroles_dashboard_title_placeholder")
        channelTitleContainer.add(titleTextfield)
        container.add(channelTitleContainer)

        val descTextfield =
            DashboardMultiLineTextField(getString(Category.UTILITY, "reactionroles_state3_mdescription"), 0, ReactionRolesCommand.DESC_LENGTH_MAX) {
                desc = it.data
                ActionResult()
            }
        if (!desc.isEmpty()) {
            descTextfield.value = desc
        }
        descTextfield.editButton = false
        descTextfield.placeholder = getString(Category.UTILITY, "reactionroles_dashboard_desc_placeholder")
        container.add(descTextfield, DashboardSeparator())

        val roleRemovementSwitch = DashboardSwitch(getString(Category.UTILITY, "reactionroles_state3_mroleremove")) {
            roleRemovement = it.data
            ActionResult()
        }
        roleRemovementSwitch.subtitle = getString(Category.UTILITY, "reactionroles_dashboard_roleremovement_help")
        roleRemovementSwitch.isChecked = roleRemovement
        container.add(roleRemovementSwitch, DashboardSeparator())

        val multipleRolesSwitch = DashboardSwitch(getString(Category.UTILITY, "reactionroles_state3_mmultipleroles")) {
            multipleRoles = it.data
            ActionResult()
        }
        multipleRolesSwitch.subtitle = getString(Category.UTILITY, "reactionroles_dashboard_multipleroles_help")
        multipleRolesSwitch.isChecked = multipleRoles
        container.add(multipleRolesSwitch, DashboardSeparator())

        val rows = slots
            .map {
                val values = arrayOf(it.emoji.formatted, it.roleName)
                GridRow(it.emoji.formatted, values)
            }

        val headers = getString(Category.UTILITY, "reactionroles_dashboard_slots_header").split('\n').toTypedArray()
        val slotGrid = DashboardGrid(headers, rows) {
            slots.removeIf { slot -> slot.emoji.formatted == it.data }
            ActionResult()
                .withRedraw()
        }
        slotGrid.rowButton = getString(Category.UTILITY, "reactionroles_dashboard_slots_button")
        container.add(DashboardText(getString(Category.UTILITY, "reactionroles_state3_mshortcuts")), slotGrid, generateSlotAddContainer(guild), DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.UTILITY, "reactionroles_dashboard_includedimage"), "reactionroles") {
            image = it.data
            ActionResult()
                .withRedraw()
        }
        container.add(imageUpload)

        if (image != null) {
            container.add(DashboardImage(image))
            val removeImageButton = DashboardButton(getString(Category.UTILITY, "reactionroles_dashboard_removeimage")) {
                image = null
                ActionResult()
                    .withRedraw()
            }
            container.add(HorizontalContainer(removeImageButton, HorizontalPusher()))
        }
        container.add(DashboardSeparator())

        val buttonContainer = HorizontalContainer()
        buttonContainer.allowWrap = true

        val sendButton = DashboardButton(getString(Category.UTILITY, "reactionroles_dashboard_send", editMode)) {
            if (channelId == null) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_dashboard_nochannel"))
            }

            val textChannel = guild.getTextChannelById(channelId!!)
            val emojiConnections = slots
                .map { EmojiConnection(it.emoji, it.roleMention) }

            val errorMessage = ReactionRoles.sendMessage(locale, textChannel, title, desc, emojiConnections, roleRemovement, multipleRoles,
                image, editMode, messageId ?: 0L)
            if (errorMessage != null) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(errorMessage)
            }

            if (editMode) {
                switchMode(false)
                ActionResult()
                    .withSuccessMessage(getString(Category.UTILITY, "reactionroles_state9_description"))
                    .withRedrawScrollToTop()
            } else {
                switchMode(false)
                ActionResult()
                    .withSuccessMessage(getString(Category.UTILITY, "reactionroles_state9_description"))
                    .withRedraw()
            }
        }
        sendButton.style = DashboardButton.Style.PRIMARY
        buttonContainer.add(sendButton)

        if (editMode) {
            val cancelButton = DashboardButton(getString(TextManager.GENERAL, "process_abort")) {
                switchMode(false)
                ActionResult()
                    .withRedrawScrollToTop()
            }
            buttonContainer.add(cancelButton)
        }

        buttonContainer.add(HorizontalPusher())
        container.add(buttonContainer)
        return container
    }

    private fun generateSlotAddContainer(guild: Guild): HorizontalContainer {
        val slotAddContainer = HorizontalContainer()
        slotAddContainer.allowWrap = true
        slotAddContainer.alignment = HorizontalContainer.Alignment.BOTTOM

        val emojiField = DashboardTextField(getString(Category.UTILITY, "reactionroles_dashboard_slots_emoji"), 0, 100) {
            if (it.data.isBlank()) {
                newSlotEmoji = null
                return@DashboardTextField ActionResult()
            }

            val emojis = MentionUtil.getEmojis(guild, it.data).list
            if (emojis.isEmpty()) {
                newSlotEmoji = null
                ActionResult()
            } else {
                this.newSlotEmoji = emojis[0]
                ActionResult()
            }
        }
        emojiField.editButton = false
        emojiField.placeholder = getString(Category.UTILITY, "reactionroles_dashboard_emojiplaceholder")
        if (newSlotEmoji != null) {
            emojiField.value = newSlotEmoji!!.formatted
        }
        slotAddContainer.add(emojiField)

        val roleField =
            DashboardRoleComboBox(getString(Category.UTILITY, "reactionroles_dashboard_slots_role"), locale, atomicGuild.idLong, atomicMember.idLong, newSlotRole?.idLong, false, true) {
                this.newSlotRole = guild.getRoleById(it.data)
                ActionResult()
            }
        slotAddContainer.add(roleField)

        val addButton = DashboardButton(getString(Category.UTILITY, "reactionroles_dashboard_slots_add")) {
            if (newSlotEmoji == null) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_dashboard_noemoji"))
            }
            if (newSlotEmoji is CustomEmoji && !ShardManager.customEmojiIsKnown(newSlotEmoji as CustomEmoji)) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(TextManager.GENERAL, "emojiunknown"))
            }
            if (slots.size >= ReactionRolesCommand.MAX_SLOTS) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_toomanyshortcuts", ReactionRolesCommand.MAX_SLOTS.toString()))
            }
            if (slots.any { it.emoji.formatted == newSlotEmoji!!.formatted }) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_emojialreadyexists"))
            }
            if (newSlotRole == null) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_dashboard_norole"))
            }
            slots += Slot(newSlotEmoji!!, "@${newSlotRole!!.name}", newSlotRole!!.asMention)
            newSlotEmoji = null
            newSlotRole = null
            ActionResult()
                .withRedraw()
        }
        addButton.setCanExpand(false)
        slotAddContainer.add(addButton)
        return slotAddContainer
    }

    private fun switchMode(editMode: Boolean) {
        this.editMode = editMode
        if (!editMode) {
            channelId = null
            title = ""
            desc = ""
            roleRemovement = true
            multipleRoles = true
            image = null
            messageId = null
            slots.clear()
            newSlotRole = null
            newSlotEmoji = null
        }
    }

    private fun readValuesFromReactionMessage(guild: Guild, reactionRoleMessage: ReactionMessage) {
        this.channelId = reactionRoleMessage.standardGuildMessageChannelId
        this.title = reactionRoleMessage.title
        this.desc = reactionRoleMessage.description.orElse("")
        this.roleRemovement = reactionRoleMessage.isRemoveRole
        this.multipleRoles = reactionRoleMessage.isMultipleRoles
        this.image = reactionRoleMessage.banner.orElse(null)
        this.messageId = reactionRoleMessage.messageId
        val newSlots = reactionRoleMessage.emojiConnections
            .map {
                val roleId = it.connection.substring(3, it.connection.length - 1)
                val role = guild.getRoleById(roleId)
                val roleName = if (role != null) {
                    "@${role.name}"
                } else {
                    getString(TextManager.GENERAL, "notfound", StringUtil.numToHex(roleId.toLong()))
                }
                Slot(
                    it.emoji,
                    roleName,
                    it.connection
                )
            }
            .filter { !it.roleName.isEmpty() }
        this.slots = ArrayList(newSlots)
        this.newSlotRole = null
        this.newSlotEmoji = null
    }

    data class Slot(val emoji: Emoji, val roleName: String, val roleMention: String) {

    }

}