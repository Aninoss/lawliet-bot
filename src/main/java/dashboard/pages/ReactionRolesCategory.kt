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
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import dashboard.data.GridRow
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

    var configuration = ReactionRoleEntity()
    lateinit var previousTitle: String
    lateinit var slotConfiguration: ReactionRoleSlotEntity
    var editMode = false
    var imageCdn: File? = null

    val reactionRoleEntities: Map<Long, ReactionRoleEntity>
        get() = guildEntity.reactionRoles

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(ReactionRolesCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (configuration.messageGuildId == 0L) {
            reset()
        }
        if (!editMode) {
            mainContainer.add(
                    DashboardText(getString(Category.CONFIGURATION, "reactionroles_state0_description", StringUtil.numToString(ReactionRolesCommand.MAX_ROLE_MESSAGES_FREE))),
                    generateReactionRolesTable(guild)
            )
        }
        mainContainer.add(generateReactionRolesDataField(guild))
    }

    private fun generateReactionRolesTable(guild: Guild): DashboardComponent {
        val title = getString(Category.CONFIGURATION, "reactionroles_dashboard_active_title")
        val rowButton = getString(Category.CONFIGURATION, "reactionroles_dashboard_active_button")

        val container = VerticalContainer()
        container.add(DashboardTitle(title))

        val rows = reactionRoleEntities.values
                .map {
                    val values = arrayOf(it.title, it.messageChannel.getPrefixedName(locale))
                    GridRow(it.messageId.toString(), values)
                }

        val headers = getString(Category.CONFIGURATION, "reactionroles_dashboard_active_header").split('\n').toTypedArray()
        val grid = DashboardGrid(headers, rows) {
            val roleMessage = reactionRoleEntities[it.data.toLong()] ?: return@DashboardGrid ActionResult().withRedraw()

            val channel = roleMessage.messageChannel.get().orElse(null)
            if (channel == null) {
                DBStaticReactionMessages.getInstance().retrieve(guild.getIdLong())
                        .remove(roleMessage.messageId)
                return@DashboardGrid ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_messagedeleted"))
            }

            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
                val error = TextManager.getString(locale, TextManager.GENERAL, "permission_channel_history", "#" + channel.name)
                return@DashboardGrid ActionResult()
                        .withErrorMessage(error)
            }

            try {
                MessageCache.retrieveMessage(channel, roleMessage.messageId).get()
            } catch (e: ExecutionException) {
                // ignore
                DBStaticReactionMessages.getInstance().retrieve(guild.getIdLong())
                        .remove(roleMessage.messageId)
                return@DashboardGrid ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_messagedeleted"))
            }

            configuration = roleMessage.copy()
            previousTitle = configuration.title
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
            getString(Category.CONFIGURATION, "reactionroles_state2_title")
        } else {
            getString(Category.CONFIGURATION, "reactionroles_state1_title")
        }
        container.add(DashboardTitle(headerTitle))

        val channelTitleContainer = HorizontalContainer()
        channelTitleContainer.allowWrap = true

        val channelLabel = getString(Category.CONFIGURATION, "reactionroles_dashboard_channel")
        val channelComboBox = DashboardChannelComboBox(
                this,
                channelLabel,
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                if (configuration.messageChannelId == 0L) null else configuration.messageChannelId,
                false
        ) {
            if (editMode) {
                return@DashboardChannelComboBox ActionResult()
            }

            configuration.messageChannelId = it.data.toLong()
            ActionResult()
        }
        channelComboBox.isEnabled = !editMode
        channelTitleContainer.add(channelComboBox)

        val titleTextfield =
                DashboardTextField(getString(Category.CONFIGURATION, "reactionroles_state3_mtitle"), 1, ReactionRolesCommand.TITLE_LENGTH_MAX) {
                    configuration.title = it.data
                    ActionResult()
                }
        titleTextfield.value = configuration.title
        titleTextfield.editButton = false
        titleTextfield.placeholder = getString(Category.CONFIGURATION, "reactionroles_dashboard_title_placeholder")
        channelTitleContainer.add(titleTextfield)
        container.add(channelTitleContainer)

        val descTextfield =
                DashboardMultiLineTextField(getString(Category.CONFIGURATION, "reactionroles_state3_mdescription"), 0, ReactionRolesCommand.DESC_LENGTH_MAX) {
                    configuration.description = it.data
                    ActionResult()
                }
        descTextfield.value = configuration.description ?: ""
        descTextfield.editButton = false
        descTextfield.placeholder = getString(Category.CONFIGURATION, "reactionroles_dashboard_desc_placeholder")
        container.add(descTextfield, DashboardSeparator())

        val roleRemovementSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "reactionroles_state3_mroleremove")) {
            configuration.roleRemovals = it.data
            ActionResult()
        }
        roleRemovementSwitch.subtitle = getString(Category.CONFIGURATION, "reactionroles_dashboard_roleremovement_help")
        roleRemovementSwitch.isChecked = configuration.roleRemovals
        container.add(roleRemovementSwitch, DashboardSeparator())

        val multipleRolesSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "reactionroles_state3_mmultipleroles")) {
            configuration.multipleSlots = it.data
            ActionResult()
        }
        multipleRolesSwitch.subtitle = getString(Category.CONFIGURATION, "reactionroles_dashboard_multipleroles_help")
        multipleRolesSwitch.isChecked = configuration.multipleSlots
        container.add(multipleRolesSwitch, DashboardSeparator())

        val roleConnectionsSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "reactionroles_state3_mshowroleconnections")) {
            configuration.slotOverview = it.data
            ActionResult()
        }
        roleConnectionsSwitch.subtitle = getString(Category.CONFIGURATION, "reactionroles_dashboard_showroleconnections_help")
        roleConnectionsSwitch.isChecked = configuration.slotOverview
        container.add(roleConnectionsSwitch, DashboardSeparator())

        val componentsEntities = ReactionRoleEntity.ComponentType.values().mapIndexed { i, type -> DiscordEntity(i.toString(), getString(Category.CONFIGURATION, "reactionroles_componenttypes", i)) }
        val newComponentsComboBox =
                DashboardComboBox(getString(Category.CONFIGURATION, "reactionroles_state3_mnewcomponents"), componentsEntities, false, 1) {
                    configuration.componentType = ReactionRoleEntity.ComponentType.values()[it.data.toInt()]
                    ActionResult()
                            .withRedraw()
                }
        newComponentsComboBox.selectedValues = listOf(DiscordEntity(configuration.componentType.ordinal.toString(), getString(Category.CONFIGURATION, "reactionroles_componenttypes", configuration.componentType.ordinal)))
        container.add(newComponentsComboBox, DashboardSeparator())

        if (configuration.componentType != ReactionRoleEntity.ComponentType.REACTIONS) {
            val showRoleNumbersSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "reactionroles_state3_mshowrolenumbers")) {
                configuration.roleCounters = it.data
                ActionResult()
            }
            showRoleNumbersSwitch.subtitle = getString(Category.CONFIGURATION, "reactionroles_dashboard_showrolecounts_help")
            showRoleNumbersSwitch.isChecked = configuration.roleCounters
            container.add(showRoleNumbersSwitch, DashboardSeparator())
        }

        val rows = configuration.slots
                .mapIndexed { i, slot ->
                    val values = arrayOf(slot.emojiFormatted ?: "", StringUtil.shortenString(slot.customLabel ?: AtomicRole(atomicGuild.idLong, slot.roleIds[0]).getPrefixedName(locale), 50))
                    GridRow(i.toString(), values)
                }

        val headers = getString(Category.CONFIGURATION, "reactionroles_dashboard_slots_header").split('\n').toTypedArray()
        val slotGrid = DashboardGrid(headers, rows) {
            configuration.slots.removeAt(it.data.toInt())
            ActionResult()
                    .withRedraw()
        }
        slotGrid.rowButton = getString(Category.CONFIGURATION, "reactionroles_dashboard_slots_button")
        container.add(DashboardText(getString(Category.CONFIGURATION, "reactionroles_state3_mshortcuts")), slotGrid, generateSlotAddContainer(guild), DashboardSeparator())

        val roleRequirementsComboBox = DashboardMultiRolesComboBox(
                this,
                getString(Category.CONFIGURATION, "reactionroles_dashboard_rolerequirements_title"),
                { configuration.roleRequirementIds },
                true,
                ReactionRolesCommand.MAX_ROLE_REQUIREMENTS,
                false
        )
        container.add(roleRequirementsComboBox, DashboardText(getString(Category.CONFIGURATION, "reactionroles_dashboard_rolerequirements")), DashboardSeparator())

        val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "reactionroles_dashboard_includedimage"), "reactionroles") {
            configuration.imageUrl = it.data
            imageCdn?.delete()
            imageCdn = LocalFile(LocalFile.Directory.CDN, "reactionroles/${configuration.imageFilename}")
            ActionResult()
                    .withRedraw()
        }
        container.add(imageUpload)

        if (configuration.imageFilename != null) {
            container.add(DashboardImage(configuration.imageUrl))
            val removeImageButton = DashboardButton(getString(Category.CONFIGURATION, "reactionroles_dashboard_removeimage")) {
                configuration.imageFilename = null
                imageCdn?.delete()
                imageCdn = null
                ActionResult()
                        .withRedraw()
            }
            container.add(HorizontalContainer(removeImageButton, HorizontalPusher()))
        }
        container.add(DashboardSeparator())

        val buttonContainer = HorizontalContainer()
        buttonContainer.allowWrap = true

        val sendButton = DashboardButton(getString(Category.CONFIGURATION, "reactionroles_dashboard_send", editMode)) {
            if (configuration.messageChannelId == 0L) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "reactionroles_dashboard_nochannel"))
            }

            val error = ReactionRoles.checkForErrors(locale, guildEntity, configuration, editMode)
            if (error != null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(error)
            }

            imageCdn = null
            entityManager.transaction.begin()
            ReactionRoles.sendMessage(guildEntity.locale, configuration, editMode, guildEntity)

            if (editMode) {
                BotLogEntity.log(entityManager, BotLogEntity.Event.REACTION_ROLES_EDIT, atomicMember, previousTitle)
                entityManager.transaction.commit()

                switchMode(false)
                ActionResult()
                        .withSuccessMessage(getString(Category.CONFIGURATION, "reactionroles_sent"))
                        .withRedrawScrollToTop()
            } else {
                BotLogEntity.log(entityManager, BotLogEntity.Event.REACTION_ROLES_ADD, atomicMember, configuration.title)
                entityManager.transaction.commit()

                switchMode(false)
                ActionResult()
                        .withSuccessMessage(getString(Category.CONFIGURATION, "reactionroles_sent"))
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

        val emojiField = DashboardTextField(getString(Category.CONFIGURATION, "reactionroles_addslot_emoji"), 0, 100) {
            slotConfiguration.emojiFormatted = it.data
            ActionResult()
        }
        emojiField.editButton = false
        emojiField.placeholder = getString(Category.CONFIGURATION, "reactionroles_dashboard_emojiplaceholder")
        emojiField.value = slotConfiguration.emojiFormatted ?: ""
        slotAddContainer.add(emojiField)

        val rolesField = DashboardMultiRolesComboBox(
                this,
                getString(Category.CONFIGURATION, "reactionroles_addslot_roles"),
                { slotConfiguration.roleIds },
                false,
                ReactionRolesCommand.MAX_ROLES,
                true
        )
        slotAddContainer.add(rolesField)

        val customLabelField = DashboardTextField(getString(Category.CONFIGURATION, "reactionroles_addslot_customlabel"), 0, ReactionRolesCommand.CUSTOM_LABEL_MAX_LENGTH) {
            slotConfiguration.customLabel = it.data
            ActionResult()
        }
        customLabelField.editButton = false
        customLabelField.placeholder = getString(Category.CONFIGURATION, "reactionroles_dashboard_customlabelplaceholder")
        customLabelField.value = slotConfiguration.customLabel ?: ""
        slotAddContainer.add(customLabelField)

        val addButton = DashboardButton(getString(Category.CONFIGURATION, "reactionroles_dashboard_slots_add")) {
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
            if (configuration.slots.size >= ReactionRolesCommand.MAX_SLOTS_TOTAL) {
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
            configuration.slots += slotConfiguration.copy()
            slotConfiguration = ReactionRoleSlotEntity()
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
            reset()
        }
        imageCdn?.delete()
        imageCdn = null
    }

    private fun reset() {
        configuration = ReactionRoleEntity()
        configuration.messageGuildId = atomicGuild.idLong
        configuration.title = Command.getCommandLanguage(ReactionRolesCommand::class.java, locale).title
        previousTitle = configuration.title
        slotConfiguration = ReactionRoleSlotEntity()
    }

}