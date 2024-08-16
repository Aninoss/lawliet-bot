package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.ReactionRolesCommand
import core.LocalFile
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicRole
import core.cache.MessageCache
import core.utils.BotPermissionUtil
import core.utils.MentionUtil
import core.utils.StringUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardChannelComboBox
import dashboard.components.DashboardEmojiComboBox
import dashboard.components.DashboardListContainerPaginated
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import modules.ReactionRoles
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.ReactionRoleEntity
import mysql.hibernate.entity.ReactionRoleSlotEntity
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.staticreactionmessages.DBStaticReactionMessages
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import java.io.File
import java.util.*
import java.util.concurrent.ExecutionException

@DashboardProperties(
        id = "reactionroles",
        botPermissions = [Permission.MANAGE_ROLES, Permission.MESSAGE_HISTORY],
        userPermissions = [Permission.MANAGE_ROLES],
        commandAccessRequirements = [ReactionRolesCommand::class]
)
class ReactionRolesCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var activeListPage = 0
    var config = ReactionRoleEntity()
    lateinit var previousTitle: String
    lateinit var slotConfiguration: ReactionRoleSlotEntity
    var editMode = false
    var slotsPage = 0
    var slotEditMode = false
    var slotEditPosition = 0
    var imageCdn: File? = null

    val reactionRoleEntities: Map<Long, ReactionRoleEntity>
        get() = guildEntity.reactionRoles

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(ReactionRolesCommand::class.java, locale).title
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "reactionroles_state0_description", StringUtil.numToString(ReactionRolesCommand.MAX_ROLE_MESSAGES_FREE))
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (config.messageGuildId == 0L) {
            reset()
        }
        if (!editMode && !slotEditMode) {
            if (reactionRoleEntities.isNotEmpty()) {
                mainContainer.add(
                        DashboardTitle(getString(Category.CONFIGURATION, "reactionroles_dashboard_active_title")),
                        generateReactionRolesListContainer(guild)
                )
            }
        }

        if (slotEditMode) {
            mainContainer.add(
                    DashboardTitle(getString(Category.CONFIGURATION, "reactionroles_state7_title")),
                    generateSlotAddContainer(guild)
            )
        } else {
            val headerTitle = if (editMode) {
                getString(Category.CONFIGURATION, "reactionroles_state2_title")
            } else {
                getString(Category.CONFIGURATION, "reactionroles_state1_title")
            }

            mainContainer.add(
                    DashboardTitle(headerTitle),
                    generateReactionRolesDataField(),
                    generateSlotsField(guild)
            )
        }
    }

    private fun generateReactionRolesListContainer(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val items = reactionRoleEntities.values
                .map { reactionRole ->
                    val button = DashboardButton(getString(Category.CONFIGURATION, "reactionroles_dashboard_active_button")) {
                        val roleMessage = reactionRoleEntities[reactionRole.messageId]
                                ?: return@DashboardButton ActionResult().withRedraw()

                        val channel = roleMessage.messageChannel.get().orElse(null)
                        if (channel == null) {
                            DBStaticReactionMessages.getInstance().retrieve(guild.getIdLong())
                                    .remove(roleMessage.messageId)
                            return@DashboardButton ActionResult()
                                    .withRedraw()
                                    .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_messagedeleted"))
                        }

                        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
                            val error = TextManager.getString(locale, TextManager.GENERAL, "permission_channel_history", "#" + channel.name)
                            return@DashboardButton ActionResult()
                                    .withErrorMessage(error)
                        }

                        try {
                            MessageCache.retrieveMessage(channel, roleMessage.messageId).get()
                        } catch (e: ExecutionException) {
                            // ignore
                            DBStaticReactionMessages.getInstance().retrieve(guild.getIdLong())
                                    .remove(roleMessage.messageId)
                            return@DashboardButton ActionResult()
                                    .withRedraw()
                                    .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_messagedeleted"))
                        }

                        config = roleMessage.copy()
                        previousTitle = config.title
                        slotConfiguration = ReactionRoleSlotEntity()
                        switchMode(true)
                        return@DashboardButton ActionResult()
                                .withRedrawScrollToTop()
                    }

                    val itemContainer = HorizontalContainer(
                            DashboardText("${reactionRole.messageChannel.getPrefixedName(locale)}: ${reactionRole.title}"),
                            HorizontalPusher(),
                            button
                    )
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    return@map itemContainer
                }

        return DashboardListContainerPaginated(items, activeListPage) { activeListPage = it }
    }

    private fun generateReactionRolesDataField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val channelTitleContainer = HorizontalContainer()
        channelTitleContainer.allowWrap = true

        val channelLabel = getString(Category.CONFIGURATION, "reactionroles_dashboard_channel")
        val channelComboBox = DashboardChannelComboBox(
                this,
                channelLabel,
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                if (config.messageChannelId == 0L) null else config.messageChannelId,
                false,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
        ) {
            if (editMode) {
                return@DashboardChannelComboBox ActionResult()
            }

            config.messageChannelId = it.data.toLong()
            ActionResult()
        }
        channelComboBox.isEnabled = !editMode
        channelTitleContainer.add(channelComboBox)

        val titleTextfield =
                DashboardTextField(getString(Category.CONFIGURATION, "reactionroles_state3_mtitle"), 1, ReactionRolesCommand.TITLE_LENGTH_MAX) {
                    config.title = it.data
                    ActionResult()
                }
        titleTextfield.value = config.title
        titleTextfield.editButton = false
        titleTextfield.placeholder = getString(Category.CONFIGURATION, "reactionroles_dashboard_title_placeholder")
        channelTitleContainer.add(titleTextfield)
        container.add(channelTitleContainer)

        val descTextfield =
                DashboardMultiLineTextField(getString(Category.CONFIGURATION, "reactionroles_state3_mdescription"), 0, ReactionRolesCommand.DESC_LENGTH_MAX) {
                    config.description = it.data
                    ActionResult()
                }
        descTextfield.value = config.description ?: ""
        descTextfield.editButton = false
        descTextfield.placeholder = getString(Category.CONFIGURATION, "reactionroles_dashboard_desc_placeholder")
        container.add(descTextfield, DashboardSeparator())

        val roleRemovementSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "reactionroles_state3_mroleremove")) {
            config.roleRemovals = it.data
            ActionResult()
        }
        roleRemovementSwitch.subtitle = getString(Category.CONFIGURATION, "reactionroles_dashboard_roleremovement_help")
        roleRemovementSwitch.isChecked = config.roleRemovals
        container.add(roleRemovementSwitch, DashboardSeparator())

        val multipleSlotsSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "reactionroles_state3_mmultipleroles")) {
            config.multipleSlots = it.data
            ActionResult()
        }
        multipleSlotsSwitch.subtitle = getString(Category.CONFIGURATION, "reactionroles_dashboard_multipleroles_help")
        multipleSlotsSwitch.isChecked = config.multipleSlots
        container.add(multipleSlotsSwitch, DashboardSeparator())

        val roleConnectionsSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "reactionroles_state3_mshowroleconnections")) {
            config.slotOverview = it.data
            ActionResult()
        }
        roleConnectionsSwitch.subtitle = getString(Category.CONFIGURATION, "reactionroles_dashboard_showroleconnections_help")
        roleConnectionsSwitch.isChecked = config.slotOverview
        container.add(roleConnectionsSwitch, DashboardSeparator())

        val componentsEntities = ReactionRoleEntity.ComponentType.values().mapIndexed { i, type -> DiscordEntity(i.toString(), getString(Category.CONFIGURATION, "reactionroles_componenttypes", i)) }
        val newComponentsComboBox =
                DashboardComboBox(getString(Category.CONFIGURATION, "reactionroles_state3_mnewcomponents"), componentsEntities, false, 1) {
                    config.componentType = ReactionRoleEntity.ComponentType.values()[it.data.toInt()]
                    ActionResult()
                            .withRedraw()
                }
        newComponentsComboBox.selectedValues = listOf(DiscordEntity(config.componentType.ordinal.toString(), getString(Category.CONFIGURATION, "reactionroles_componenttypes", config.componentType.ordinal)))
        container.add(
                newComponentsComboBox,
                DashboardText(getString(Category.CONFIGURATION, "reactionroles_dashboard_componenttype_hint"), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        if (config.componentType != ReactionRoleEntity.ComponentType.REACTIONS) {
            val showRoleNumbersSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "reactionroles_state3_mshowrolenumbers")) {
                config.roleCounters = it.data
                ActionResult()
            }
            showRoleNumbersSwitch.subtitle = getString(Category.CONFIGURATION, "reactionroles_dashboard_showrolecounts_help")
            showRoleNumbersSwitch.isChecked = config.roleCounters
            container.add(showRoleNumbersSwitch, DashboardSeparator())
        }

        val roleRequirementsComboBox = DashboardMultiRolesComboBox(
                this,
                getString(Category.CONFIGURATION, "reactionroles_dashboard_rolerequirements_title"),
                { config.roleRequirementIds },
                true,
                ReactionRolesCommand.MAX_ROLE_REQUIREMENTS,
                false
        )
        container.add(
                roleRequirementsComboBox,
                DashboardText(getString(Category.CONFIGURATION, "reactionroles_dashboard_rolerequirements"), DashboardText.Style.HINT),
                DashboardSeparator()
        )

        val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "reactionroles_dashboard_includedimage"), "reactionroles", 1) { e ->
            if (e.type == "add") {
                config.imageUrl = e.data
                imageCdn?.delete()
                imageCdn = LocalFile(LocalFile.Directory.CDN, "reactionroles/${config.imageFilename}")
            } else if (e.type == "remove") {
                config.imageFilename = null
                imageCdn?.delete()
                imageCdn = null
            }
            return@DashboardImageUpload ActionResult()
                    .withRedraw()
        }
        if (config.imageFilename != null) {
            imageUpload.values = listOf(config.imageUrl)
        }
        container.add(imageUpload, DashboardSeparator())

        val buttonContainer = HorizontalContainer()
        buttonContainer.allowWrap = true

        val sendButton = DashboardButton(getString(Category.CONFIGURATION, "reactionroles_dashboard_send", editMode)) {
            if (config.messageChannelId == 0L) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_dashboard_nochannel"))
            }

            val error = ReactionRoles.checkForErrors(locale, guildEntity, config, editMode)
            if (error != null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(error)
            }

            imageCdn = null
            entityManager.transaction.begin()
            ReactionRoles.sendMessage(guildEntity.locale, config, editMode, guildEntity)

            if (editMode) {
                BotLogEntity.log(entityManager, BotLogEntity.Event.REACTION_ROLES_EDIT, atomicMember, previousTitle)
                entityManager.transaction.commit()

                switchMode(false)
                ActionResult()
                        .withSuccessMessage(getString(Category.CONFIGURATION, "reactionroles_sent"))
                        .withRedrawScrollToTop()
            } else {
                BotLogEntity.log(entityManager, BotLogEntity.Event.REACTION_ROLES_ADD, atomicMember, config.title)
                entityManager.transaction.commit()

                switchMode(false)
                ActionResult()
                        .withSuccessMessage(getString(Category.CONFIGURATION, "reactionroles_sent"))
                        .withRedrawScrollToTop()
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

    private fun generateSlotsField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()

        val slotText = DashboardText(getString(Category.CONFIGURATION, "reactionroles_state3_mshortcuts"))
        slotText.putCssProperties("margin-top", "1.25rem")
        container.add(slotText)

        val items = config.slots
                .mapIndexed { i, slot ->
                    val atomicRole = AtomicRole(atomicGuild.idLong, slot.roleIds[0])
                    val editButton = DashboardButton("🖊️") {
                        slotConfiguration = config.slots[i].copy()
                        slotEditMode = true
                        slotEditPosition = i
                        return@DashboardButton ActionResult()
                                .withRedrawScrollToTop()
                    }

                    val upButton = DashboardButton("🡑") {
                        config.slots.add(i - 1, config.slots.removeAt(i))
                        return@DashboardButton ActionResult()
                                .withRedraw()
                    }
                    upButton.style = DashboardButton.Style.TERTIARY
                    upButton.isEnabled = i != 0

                    val downButton = DashboardButton("🡓") {
                        config.slots.add(i + 1, config.slots.removeAt(i))
                        return@DashboardButton ActionResult()
                                .withRedraw()
                    }
                    downButton.style = DashboardButton.Style.TERTIARY
                    downButton.isEnabled = i != config.slots.size - 1

                    val itemContainer = HorizontalContainer(
                            DashboardText(if (slot.customLabel != null) slot.customLabel else atomicRole.getPrefixedName(locale)),
                            HorizontalPusher(),
                            upButton,
                            editButton,
                            downButton
                    )
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    return@mapIndexed itemContainer
                }
        if (items.isNotEmpty()) {
            val listContainer = DashboardListContainerPaginated(items, slotsPage) { slotsPage = it }
            container.add(listContainer)
        }

        container.add(generateSlotAddContainer(guild))
        return container
    }

    private fun generateSlotAddContainer(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val propertiesContainer = HorizontalContainer()
        propertiesContainer.allowWrap = true
        propertiesContainer.alignment = HorizontalContainer.Alignment.BOTTOM

        val emojiSelect = DashboardEmojiComboBox(
                getString(Category.CONFIGURATION, "reactionroles_addslot_emoji"),
                slotConfiguration.emojiFormatted,
                true
        ) {
            slotConfiguration.emojiFormatted = it.data
            ActionResult()
        }
        emojiSelect.placeholder = getString(Category.CONFIGURATION, "reactionroles_dashboard_emojiplaceholder")
        propertiesContainer.add(emojiSelect)

        val rolesField = DashboardMultiRolesComboBox(
                this,
                getString(Category.CONFIGURATION, "reactionroles_addslot_roles"),
                { slotConfiguration.roleIds },
                false,
                ReactionRolesCommand.MAX_ROLES,
                true
        )
        propertiesContainer.add(rolesField)
        container.add(propertiesContainer)

        val customLabelField = DashboardTextField(getString(Category.CONFIGURATION, "reactionroles_addslot_customlabel"), 0, ReactionRolesCommand.CUSTOM_LABEL_MAX_LENGTH) {
            slotConfiguration.customLabel = if (it.data.isNotEmpty()) it.data else null
            ActionResult()
        }
        customLabelField.editButton = false
        customLabelField.placeholder = getString(Category.CONFIGURATION, "reactionroles_dashboard_customlabelplaceholder")
        customLabelField.value = slotConfiguration.customLabel ?: ""
        container.add(
                customLabelField,
                DashboardText(getString(Category.CONFIGURATION, "reactionroles_dashboard_customlabel_hint"), DashboardText.Style.HINT),
                DashboardSeparator(true)
        )

        val buttonContainer = HorizontalContainer()
        val addButton = DashboardButton(getString(Category.CONFIGURATION, "reactionroles_dashboard_slots_add", slotEditMode)) {
            val emojis = MentionUtil.getEmojis(guild, slotConfiguration.emojiFormatted ?: "").list
            val emoji = if (emojis.isEmpty()) {
                null
            } else {
                emojis[0]
            }
            if (slotConfiguration.emojiFormatted != null && emoji == null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_dashboard_noemoji"))
            }
            if (emoji != null && emoji is CustomEmoji && !ShardManager.customEmojiIsKnown(emoji)) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(TextManager.GENERAL, "emojiunknown", emoji.name))
            }
            if (!slotEditMode && config.slots.size >= ReactionRolesCommand.MAX_SLOTS_TOTAL) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_toomanyshortcuts", ReactionRolesCommand.MAX_SLOTS_TOTAL.toString()))
            }
            if (slotConfiguration.roleIds.isEmpty()) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_dashboard_norole"))
            }
            if (slotConfiguration.roleIds.size > 1 && slotConfiguration.customLabel == null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_addslot_nocustomlabel"))
            }

            slotConfiguration.emojiFormatted = emoji?.formatted
            if (slotEditMode) {
                config.slots[slotEditPosition] = slotConfiguration.copy()
            } else {
                config.slots += slotConfiguration.copy()
            }

            slotConfiguration = ReactionRoleSlotEntity()
            slotEditMode = false
            ActionResult()
                    .withRedraw()
        }
        buttonContainer.add(addButton)

        if (slotEditMode) {
            addButton.style = DashboardButton.Style.PRIMARY

            val removeButton = DashboardButton(getString(Category.CONFIGURATION, "reactionroles_dashboard_slots_remove")) {
                config.slots.removeAt(slotEditPosition)
                slotConfiguration = ReactionRoleSlotEntity()
                slotEditMode = false
                return@DashboardButton ActionResult()
                        .withRedrawScrollToTop()
            }
            removeButton.style = DashboardButton.Style.DANGER
            buttonContainer.add(removeButton)

            val cancelButton = DashboardButton(getString(Category.CONFIGURATION, "reactionroles_dashboard_slots_cancel")) {
                slotConfiguration = ReactionRoleSlotEntity()
                slotEditMode = false
                return@DashboardButton ActionResult()
                        .withRedrawScrollToTop()
            }
            buttonContainer.add(cancelButton)
        }

        buttonContainer.add(HorizontalPusher())
        container.add(buttonContainer)
        return container
    }

    private fun switchMode(editMode: Boolean) {
        this.editMode = editMode
        if (!editMode) {
            reset()
        }
        imageCdn?.delete()
        imageCdn = null
        slotsPage = 0
    }

    private fun reset() {
        config = ReactionRoleEntity()
        config.messageGuildId = atomicGuild.idLong
        config.title = Command.getCommandLanguage(ReactionRolesCommand::class.java, locale).title
        previousTitle = config.title
        slotConfiguration = ReactionRoleSlotEntity()
    }

}