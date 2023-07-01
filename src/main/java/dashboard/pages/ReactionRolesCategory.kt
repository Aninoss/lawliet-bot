package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.ReactionRolesCommand
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicRole
import core.atomicassets.AtomicStandardGuildMessageChannel
import core.cache.MessageCache
import core.utils.BotPermissionUtil
import core.utils.MentionUtil
import core.utils.StringUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.components.DashboardRoleComboBox
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import dashboard.data.GridRow
import modules.ReactionRoles
import mysql.hibernate.entity.GuildEntity
import mysql.modules.reactionroles.ReactionRoleMessage
import mysql.modules.reactionroles.ReactionRoleMessage.ComponentType
import mysql.modules.reactionroles.ReactionRoleMessageSlot
import mysql.modules.staticreactionmessages.DBStaticReactionMessages
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@DashboardProperties(
    id = "reactionroles",
    botPermissions = [Permission.MANAGE_ROLES, Permission.MESSAGE_HISTORY],
    userPermissions = [Permission.MANAGE_ROLES],
    commandAccessRequirements = [ReactionRolesCommand::class]
)
class ReactionRolesCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var channelId: Long? = null
    var title = ""
    var desc = ""
    var roleRemovement = true
    var multipleRoles = true
    var showRoleConnections = true
    var newComponents = ComponentType.REACTIONS
    var showRoleNumbers = false
    var image: String? = null
    var messageId: Long? = null
    var slots: ArrayList<Slot> = ArrayList()
    var roleRequirements: ArrayList<Long> = ArrayList()

    var newSlotEmoji: String = ""
    var newSlotRole: Role? = null
    var newSlotCustomLabel: String = ""

    var editMode = false

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(ReactionRolesCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val reactionMessages: List<ReactionRoleMessage> = ReactionRoles.getReactionMessagesInGuild(atomicGuild.idLong)

        if (!editMode) {
            mainContainer.add(generateReactionRolesTable(guild, reactionMessages))
        }

        mainContainer.add(generateReactionRolesDataField(guild))
    }

    private fun generateReactionRolesTable(guild: Guild, reactionMessages: List<ReactionRoleMessage>): DashboardComponent {
        val title = getString(Category.UTILITY, "reactionroles_dashboard_active_title")
        val rowButton = getString(Category.UTILITY, "reactionroles_dashboard_active_button")

        val container = VerticalContainer()
        container.add(DashboardTitle(title))

        val rows = reactionMessages
            .map {
                val atomicChannel =
                    AtomicStandardGuildMessageChannel(guild.idLong, it.standardGuildMessageChannelId)
                val values = arrayOf(it.title, atomicChannel.getPrefixedName(locale))
                GridRow(it.messageId.toString(), values)
            }

        val headers = getString(Category.UTILITY, "reactionroles_dashboard_active_header").split('\n').toTypedArray()
        val grid = DashboardGrid(headers, rows) {
            val reactionRoleMessage = ReactionRoles.getReactionMessagesInGuild(atomicGuild.idLong)
                .filter { m -> m.messageId == it.data.toLong() }
                .firstOrNull() ?: return@DashboardGrid ActionResult()
                    .withRedraw()

            val channel = reactionRoleMessage.standardGuildMessageChannel.get()
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
                val error = TextManager.getString(locale, TextManager.GENERAL, "permission_channel_history", "#" + channel.name)
                return@DashboardGrid ActionResult()
                    .withErrorMessage(error)
            }

            try {
                MessageCache.retrieveMessage(channel, reactionRoleMessage.messageId).get()
            } catch (e: ExecutionException) {
                // ignore
                DBStaticReactionMessages.getInstance().retrieve(guild.getIdLong())
                    .remove(reactionRoleMessage.messageId)
                return@DashboardGrid ActionResult()
                    .withRedraw()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_messagedeleted"))
            }

            readValuesFromReactionMessage(guild, reactionRoleMessage)
            switchMode(true)
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

        val premiumNotification = getString(Category.UTILITY, "reactionroles_dashboard_desc", StringUtil.numToString(ReactionRolesCommand.MAX_NEW_COMPONENTS_MESSAGES))
        container.add(DashboardText(premiumNotification))

        val channelTitleContainer = HorizontalContainer()
        channelTitleContainer.allowWrap = true

        val channelLabel = getString(Category.UTILITY, "reactionroles_dashboard_channel")
        val channelComboBox = DashboardTextChannelComboBox(channelLabel, locale, guild.idLong, channelId, false) {
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

        val roleConnectionsSwitch = DashboardSwitch(getString(Category.UTILITY, "reactionroles_state3_mshowroleconnections")) {
            showRoleConnections = it.data
            ActionResult()
        }
        roleConnectionsSwitch.subtitle = getString(Category.UTILITY, "reactionroles_dashboard_showroleconnections_help")
        roleConnectionsSwitch.isChecked = showRoleConnections
        container.add(roleConnectionsSwitch, DashboardSeparator())

        val componentsEntities =
            ComponentType.values().mapIndexed { i, type -> DiscordEntity(i.toString(), getString(Category.UTILITY, "reactionroles_componenttypes", i)) }
        val newComponentsComboBox =
            DashboardComboBox(getString(Category.UTILITY, "reactionroles_state3_mnewcomponents"), componentsEntities, false, 1) {
                newComponents = ComponentType.values()[it.data.toInt()]
                ActionResult()
                    .withRedraw()
            }
        newComponentsComboBox.selectedValues =
            listOf(DiscordEntity(newComponents.ordinal.toString(), getString(Category.UTILITY, "reactionroles_componenttypes", newComponents.ordinal)))
        container.add(newComponentsComboBox, DashboardSeparator())

        if (newComponents != ComponentType.REACTIONS) {
            val showRoleNumbersSwitch = DashboardSwitch(getString(Category.UTILITY, "reactionroles_state3_mshowrolenumbers")) {
                showRoleNumbers = it.data
                ActionResult()
            }
            showRoleNumbersSwitch.subtitle = getString(Category.UTILITY, "reactionroles_dashboard_showrolecounts_help")
            showRoleNumbersSwitch.isChecked = showRoleNumbers
            container.add(showRoleNumbersSwitch, DashboardSeparator())
        }

        val rows = slots
            .mapIndexed { i, slot ->
                val values = arrayOf(slot.emoji?.formatted ?: "", slot.roleName, slot.customLabel ?: "")
                GridRow(i.toString(), values)
            }

        val headers = getString(Category.UTILITY, "reactionroles_dashboard_slots_header").split('\n').toTypedArray()
        val slotGrid = DashboardGrid(headers, rows) {
            slots.removeAt(it.data.toInt())
            ActionResult()
                .withRedraw()
        }
        slotGrid.rowButton = getString(Category.UTILITY, "reactionroles_dashboard_slots_button")
        container.add(DashboardText(getString(Category.UTILITY, "reactionroles_state3_mshortcuts")), slotGrid, generateSlotAddContainer(guild), DashboardSeparator())

        val roleRequirementsComboBox = DashboardMultiRolesComboBox(
            getString(Category.UTILITY, "reactionroles_dashboard_rolerequirements_title"),
            locale,
            guild.idLong,
            atomicMember.idLong,
            roleRequirements,
            true,
            ReactionRolesCommand.MAX_ROLE_REQUIREMENTS,
            false
        )
        container.add(roleRequirementsComboBox, DashboardText(getString(Category.UTILITY, "reactionroles_dashboard_rolerequirements")), DashboardSeparator())

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
            val convertedSlots = slots
                .map { ReactionRoleMessageSlot(guild.idLong, it.emoji, it.roleId, it.customLabel) }

            val error = ReactionRoles.checkForErrors(locale, textChannel, convertedSlots, roleRequirements.map { AtomicRole(guild.idLong, it) }, newComponents)
            if (error != null) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(error)
            }

            val guildLocale = guildEntity.locale
            ReactionRoles.sendMessage(
                guildLocale, textChannel, title, desc, convertedSlots, roleRequirements.map { AtomicRole(guild.idLong, it) }, roleRemovement,
                multipleRoles, showRoleConnections, newComponents, showRoleNumbers, image, editMode, messageId ?: 0L
            ).get(5, TimeUnit.SECONDS)

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
            newSlotEmoji = it.data
            ActionResult()
        }
        emojiField.editButton = false
        emojiField.placeholder = getString(Category.UTILITY, "reactionroles_dashboard_emojiplaceholder")
        if (newSlotEmoji.isNotEmpty()) {
            emojiField.value = newSlotEmoji
        }
        slotAddContainer.add(emojiField)

        val roleField =
            DashboardRoleComboBox(getString(Category.UTILITY, "reactionroles_dashboard_slots_role"), locale, atomicGuild.idLong, atomicMember.idLong, newSlotRole?.idLong, false, true) {
                this.newSlotRole = guild.getRoleById(it.data)
                ActionResult()
            }
        slotAddContainer.add(roleField)

        val customLabelField = DashboardTextField(getString(Category.UTILITY, "reactionroles_dashboard_slots_customlabel"), 0, ReactionRolesCommand.CUSTOM_LABEL_MAX_LENGTH) {
            newSlotCustomLabel = it.data
            ActionResult()
        }
        customLabelField.editButton = false
        customLabelField.placeholder = getString(Category.UTILITY, "reactionroles_dashboard_customlabelplaceholder")
        if (newSlotCustomLabel.isNotEmpty()) {
            customLabelField.value = newSlotCustomLabel
        }
        slotAddContainer.add(customLabelField)

        val addButton = DashboardButton(getString(Category.UTILITY, "reactionroles_dashboard_slots_add")) {
            val emojis = MentionUtil.getEmojis(guild, newSlotEmoji).list
            val emoji = if (emojis.isEmpty()) {
                null
            } else {
                emojis[0]
            }
            if (newSlotEmoji.isNotEmpty() && emoji == null) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_dashboard_noemoji"))
            }
            if (emoji != null && emoji is CustomEmoji && !ShardManager.customEmojiIsKnown(emoji)) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(TextManager.GENERAL, "emojiunknown", emoji.name))
            }
            if (slots.size >= ReactionRolesCommand.MAX_SLOTS_TOTAL) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_toomanyshortcuts", ReactionRolesCommand.MAX_SLOTS_TOTAL.toString()))
            }
            if (newSlotRole == null) {
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "reactionroles_dashboard_norole"))
            }

            val customLabel: String? = if (newSlotCustomLabel.isEmpty()) {
                null
            } else {
                newSlotCustomLabel
            }

            slots += Slot(emoji, "@${newSlotRole!!.name}", newSlotRole!!.idLong, customLabel)
            newSlotEmoji = ""
            newSlotRole = null
            newSlotCustomLabel = ""
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
            val guildLocale = guildEntity.locale
            channelId = null
            title = Command.getCommandLanguage(ReactionRolesCommand::class.java, guildLocale).title
            desc = ""
            roleRemovement = true
            multipleRoles = true
            showRoleConnections = true
            newComponents = ComponentType.REACTIONS
            showRoleNumbers = false
            image = null
            messageId = null
            slots.clear()
            roleRequirements.clear()
            newSlotRole = null
            newSlotEmoji = ""
            newSlotCustomLabel = ""
        }
    }

    private fun readValuesFromReactionMessage(guild: Guild, reactionRoleMessage: ReactionRoleMessage) {
        this.channelId = reactionRoleMessage.standardGuildMessageChannelId
        this.title = reactionRoleMessage.title
        this.desc = reactionRoleMessage.desc ?: ""
        this.roleRemovement = reactionRoleMessage.roleRemoval
        this.multipleRoles = reactionRoleMessage.multipleRoles
        this.showRoleConnections = reactionRoleMessage.showRoleConnections
        this.newComponents = reactionRoleMessage.newComponents
        this.showRoleNumbers = reactionRoleMessage.showRoleNumbers
        this.image = reactionRoleMessage.image ?: ""
        this.messageId = reactionRoleMessage.messageId
        val newSlots = reactionRoleMessage.slots
            .map {
                val roleId = it.roleId
                val role = guild.getRoleById(roleId)
                val roleName = if (role != null) {
                    "@${role.name}"
                } else {
                    getString(TextManager.GENERAL, "notfound", StringUtil.numToHex(roleId))
                }
                Slot(
                    it.emoji,
                    roleName,
                    it.roleId,
                    it.customLabel
                )
            }
            .filter { !it.roleName.isEmpty() }
        this.slots = ArrayList(newSlots)
        this.roleRequirements = ArrayList(reactionRoleMessage.roleRequirements.map { it.idLong })
        this.newSlotRole = null
        this.newSlotEmoji = ""
        this.newSlotCustomLabel = ""
    }

    data class Slot(val emoji: Emoji?, val roleName: String, val roleId: Long, val customLabel: String?) {

    }

}