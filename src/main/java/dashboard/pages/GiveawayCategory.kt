package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.GiveawayCommand
import core.TextManager
import core.atomicassets.AtomicGuildMessageChannel
import core.utils.BotPermissionUtil
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
import modules.Giveaway
import modules.schedulers.GiveawayScheduler
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.GiveawayEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@DashboardProperties(
        id = "giveaway",
        userPermissions = [Permission.MANAGE_SERVER],
        botPermissions = [Permission.MANAGE_ROLES],
        commandAccessRequirements = [GiveawayCommand::class]
)
class GiveawayCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    enum class Mode { OVERVIEW, EDIT, REROLL }

    var ongoingListPage = 0
    var completedListPage = 0
    lateinit var config: GiveawayEntity
    var previousItem: String? = null
    var mode: Mode = Mode.OVERVIEW

    val giveawayEntities: MutableMap<Long, GiveawayEntity>
        get() = guildEntity.giveaways

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(GiveawayCommand::class.java, locale).title
    }

    override fun retrievePageDescription(): String {
        return getString(Category.CONFIGURATION, "giveaway_dashboard_desc")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (previousItem == null) {
            switchMode(Mode.OVERVIEW)
        }

        if (mode == Mode.OVERVIEW) {
            if (giveawayEntities.any { it.value.active }) {
                mainContainer.add(
                        DashboardTitle(getString(Category.CONFIGURATION, "giveaway_dashboard_ongoing_title")),
                        generateOngoingGiveawaysTable(guild)
                )
            }
            if (giveawayEntities.any { !it.value.active }) {
                mainContainer.add(
                        DashboardTitle(getString(Category.CONFIGURATION, "giveaway_dashboard_completed_title")),
                        generateCompletedGiveawaysTable(guild)
                )
            }
        }

        val title = when (mode) {
            Mode.OVERVIEW -> getString(Category.CONFIGURATION, "giveaway_state1_title")
            Mode.EDIT -> getString(Category.CONFIGURATION, "giveaway_state2_title")
            Mode.REROLL -> getString(Category.CONFIGURATION, "giveaway_state12_title")
        }
        mainContainer.add(
                DashboardTitle(title),
                generateGiveawayDataField(guild)
        )
    }

    private fun generateOngoingGiveawaysTable(guild: Guild): DashboardComponent {
        return generateGiveawaysTable(
                guild,
                getString(Category.CONFIGURATION, "giveaway_dashboard_ongoing_button"),
                { it.active },
                ongoingListPage,
                { ongoingListPage = it }
        ) {
            val giveaway = giveawayEntities.get(it)
            if (giveaway != null && giveaway.active) {
                config = giveaway.copy()
                previousItem = config.item
                switchMode(Mode.EDIT)
            }
        }
    }

    private fun generateCompletedGiveawaysTable(guild: Guild): DashboardComponent {
        return generateGiveawaysTable(
                guild,
                getString(Category.CONFIGURATION, "giveaway_dashboard_completed_button"),
                { !it.active },
                completedListPage,
                { completedListPage = it }
        ) {
            val giveaway = giveawayEntities.get(it)
            if (giveaway != null && !giveaway.active) {
                config = giveaway.copy()
                previousItem = config.item
                switchMode(Mode.REROLL)
            }
        }
    }

    private fun generateGiveawaysTable(guild: Guild, rowButton: String, filter: (GiveawayEntity) -> Boolean, page: Int, pageSwitchConsumer: (Int) -> Unit, action: (Long) -> Any): DashboardComponent {
        val items = giveawayEntities.values
                .filter(filter)
                .map { giveaway ->
                    val atomicChannel = AtomicGuildMessageChannel(guild.idLong, giveaway.channelId)
                    val editButton = DashboardButton(rowButton) {
                        action(giveaway.messageId)
                        ActionResult()
                                .withRedrawScrollToTop()
                    }

                    val itemContainer = HorizontalContainer(
                            DashboardText("${atomicChannel.getPrefixedName(locale)}: ${giveaway.item}"),
                            HorizontalPusher(),
                            editButton
                    )
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    return@map itemContainer
                }

        return DashboardListContainerPaginated(items, page, pageSwitchConsumer)
    }

    private fun generateGiveawayDataField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val channelArticleContainer = HorizontalContainer()
        channelArticleContainer.allowWrap = true

        val channelLabel = getString(Category.CONFIGURATION, "giveaway_dashboard_channel")
        val channelComboBox = DashboardChannelComboBox(
                this,
                channelLabel,
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                if (config.channelId != 0L) config.channelId else null,
                false,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
        ) {
            if (mode != Mode.OVERVIEW) {
                return@DashboardChannelComboBox ActionResult()
            }

            config.channelId = it.data.toLong()
            ActionResult()
        }
        channelComboBox.isEnabled = mode == Mode.OVERVIEW
        channelArticleContainer.add(channelComboBox)

        val articleTextfield = DashboardTextField(getString(Category.CONFIGURATION, "giveaway_state3_mtitle"), 1, GiveawayCommand.ITEM_LENGTH_MAX) {
            if (mode == Mode.REROLL) {
                return@DashboardTextField ActionResult()
            }

            config.item = it.data
            ActionResult()
        }
        articleTextfield.value = config.item
        articleTextfield.editButton = false
        articleTextfield.placeholder = getString(Category.CONFIGURATION, "giveaway_dashboard_article_placeholder")
        articleTextfield.isEnabled = mode != Mode.REROLL
        channelArticleContainer.add(articleTextfield)
        container.add(channelArticleContainer)

        val descTextfield =
                DashboardMultiLineTextField(getString(Category.CONFIGURATION, "giveaway_state3_mdescription"), 0, GiveawayCommand.DESC_LENGTH_MAX) {
                    if (mode == Mode.REROLL) {
                        return@DashboardMultiLineTextField ActionResult()
                    }

                    config.description = it.data
                    ActionResult()
                }
        if (config.description != null) {
            descTextfield.value = config.description
        }
        descTextfield.editButton = false
        descTextfield.placeholder = getString(Category.CONFIGURATION, "giveaway_dashboard_desc_placeholder")
        descTextfield.isEnabled = mode != Mode.REROLL
        container.add(descTextfield, DashboardSeparator(true))

        val durationField = DashboardDurationField(getString(Category.CONFIGURATION, "giveaway_state3_mduration")) {
            if (mode != Mode.OVERVIEW) {
                return@DashboardDurationField ActionResult()
            }

            config.durationMinutes = it.data.toInt()
            ActionResult()
        }
        durationField.value = config.durationMinutes.toLong()
        durationField.editButton = false
        durationField.isEnabled = mode == Mode.OVERVIEW
        container.add(durationField)

        val winnersEmojiRolesContainer = HorizontalContainer()
        winnersEmojiRolesContainer.allowWrap = true

        val winnersField =
                DashboardNumberField(getString(Category.CONFIGURATION, "giveaway_state3_mwinners"), GiveawayCommand.WINNERS_MIN.toLong(), GiveawayCommand.WINNERS_MAX.toLong()) {
                    config.winners = it.data.toInt()
                    ActionResult()
                }
        winnersField.value = config.winners.toLong()
        winnersField.editButton = false
        winnersEmojiRolesContainer.add(winnersField)

        val emojiComboBox = DashboardEmojiComboBox(
                getString(Category.CONFIGURATION, "giveaway_state3_memoji"),
                config.emojiFormatted,
                false
        ) {
            config.emojiFormatted = it.data
            ActionResult()
        }
        emojiComboBox.isEnabled = mode == Mode.OVERVIEW
        winnersEmojiRolesContainer.add(emojiComboBox)

        val rolesField = DashboardMultiRolesComboBox(
                this,
                getString(Category.CONFIGURATION, "giveaway_state3_mprizeroles"),
                { config.prizeRoleIds },
                true,
                GiveawayCommand.MAX_ROLE_PRIZES,
                true
        )
        rolesField.isEnabled = mode == Mode.OVERVIEW
        winnersEmojiRolesContainer.add(rolesField)

        container.add(winnersEmojiRolesContainer, DashboardSeparator(true))

        if (mode != Mode.REROLL) {
            val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "giveaway_dashboard_includedimage"), "giveaway", 1) { e ->
                if (e.type == "add") {
                    config.imageUrl = e.data
                } else if (e.type == "remove") {
                    config.imageFilename = null
                }
                return@DashboardImageUpload ActionResult()
                        .withRedraw()
            }
            if (config.imageFilename != null) {
                imageUpload.values = listOf(config.imageUrl)
            }
            container.add(imageUpload)
            container.add(DashboardSeparator(true))
        }

        val buttonContainer = HorizontalContainer()
        buttonContainer.allowWrap = true

        val sendButton = DashboardButton(getString(Category.CONFIGURATION, "giveaway_dashboard_send", mode == Mode.REROLL)) {
            if (mode != Mode.REROLL) {
                return@DashboardButton submit(guild, false)
            } else {
                val giveaway = giveawayEntities.get(config.messageId)
                if (giveaway == null) {
                    switchMode(Mode.OVERVIEW)
                    return@DashboardButton ActionResult()
                            .withRedrawScrollToTop()
                }

                val messageExists = GiveawayScheduler.processGiveawayUsers(giveaway, locale, config.winners, true)
                if (messageExists) {
                    entityManager.transaction.begin()
                    BotLogEntity.log(entityManager, BotLogEntity.Event.GIVEAWAYS_REROLL, atomicMember, config.item)
                    entityManager.transaction.commit()

                    switchMode(Mode.OVERVIEW)
                    return@DashboardButton ActionResult()
                            .withRedrawScrollToTop()
                            .withSuccessMessage(getString(Category.CONFIGURATION, "giveaway_dashboard_success", 2))
                } else {
                    entityManager.transaction.begin()
                    giveawayEntities.remove(config.messageId)
                    entityManager.transaction.commit()
                    switchMode(Mode.OVERVIEW)

                    return@DashboardButton ActionResult()
                            .withRedrawScrollToTop()
                            .withErrorMessage(getString(Category.CONFIGURATION, "giveaway_nomessage"))
                }
            }
        }
        sendButton.style = DashboardButton.Style.PRIMARY
        buttonContainer.add(sendButton)

        if (mode == Mode.EDIT) {
            val endPrematurelyButton = DashboardButton(getString(Category.CONFIGURATION, "giveaway_dashboard_endpre")) {
                return@DashboardButton submit(guild, true)
            }
            endPrematurelyButton.style = DashboardButton.Style.PRIMARY
            buttonContainer.add(endPrematurelyButton)
        }

        if (mode == Mode.REROLL) {
            val removeButton = DashboardButton(getString(Category.CONFIGURATION, "giveaway_state13_delete")) {
                entityManager.transaction.begin()
                BotLogEntity.log(entityManager, BotLogEntity.Event.GIVEAWAYS_REMOVE, atomicMember, config.item)
                guildEntity.giveaways -= config.messageId
                entityManager.transaction.commit()

                switchMode(Mode.OVERVIEW)
                ActionResult()
                        .withRedrawScrollToTop()
            }
            removeButton.style = DashboardButton.Style.DANGER
            buttonContainer.add(removeButton)
        }

        if (mode != Mode.OVERVIEW) {
            val cancelButton = DashboardButton(getString(TextManager.GENERAL, "process_abort")) {
                switchMode(Mode.OVERVIEW)
                ActionResult()
                        .withRedrawScrollToTop()
            }
            buttonContainer.add(cancelButton)
        }

        buttonContainer.add(HorizontalPusher())
        container.add(buttonContainer)
        return container
    }

    private fun submit(guild: Guild, endPrematurely: Boolean): ActionResult {
        val errorActionResponse = checkFieldValidity(guild)
        if (errorActionResponse != null) {
            return errorActionResponse
        }

        if (mode == Mode.EDIT && (!giveawayEntities.containsKey(config.messageId) || !giveawayEntities[config.messageId]!!.active)) {
            switchMode(Mode.OVERVIEW)
            return ActionResult()
                    .withErrorMessage(getString(Category.CONFIGURATION, "giveaway_dashboard_toolate"))
                    .withRedrawScrollToTop()
        }

        if (mode == Mode.OVERVIEW) {
            config.created = Instant.now()
        }

        try {
            val messageId = sendMessage(guild, config.channelId)
            config.messageId = messageId
        } catch (e: ErrorResponseException) {
            if (mode != Mode.OVERVIEW) {
                entityManager.transaction.begin()
                giveawayEntities.remove(config.messageId)
                entityManager.transaction.commit()
                switchMode(Mode.OVERVIEW)

                return ActionResult()
                        .withRedrawScrollToTop()
                        .withErrorMessage(getString(Category.CONFIGURATION, "giveaway_nomessage"))
            }
        }

        val giveaway = config.copy()
        if (endPrematurely) {
            giveaway.durationMinutes = 0
        }

        entityManager.transaction.begin()
        if (endPrematurely) {
            BotLogEntity.log(entityManager, BotLogEntity.Event.GIVEAWAYS_END, atomicMember, previousItem)
        } else {
            if (mode == Mode.OVERVIEW) {
                BotLogEntity.log(entityManager, BotLogEntity.Event.GIVEAWAYS_ADD, atomicMember, config.item)
            } else {
                BotLogEntity.log(entityManager, BotLogEntity.Event.GIVEAWAYS_EDIT, atomicMember, previousItem)
            }
        }

        val newGiveaway = giveawayEntities.put(config.messageId, giveaway) == null
        entityManager.transaction.commit()

        if (endPrematurely || newGiveaway) {
            GiveawayScheduler.loadGiveaway(giveaway)
            if (endPrematurely) {
                TimeUnit.SECONDS.sleep(3)
            }
        }

        val actionResult = ActionResult()
                .withRedrawScrollToTop()
                .withSuccessMessage(getString(Category.CONFIGURATION, "giveaway_dashboard_success", mode != Mode.OVERVIEW))
        switchMode(Mode.OVERVIEW)
        return actionResult
    }

    private fun checkFieldValidity(guild: Guild): ActionResult? {
        val channel = guild.getChannelById(GuildMessageChannel::class.java, config.channelId.toString())
        if (channel == null) { /* invalid channel */
            return ActionResult()
                    .withErrorMessage(getString(Category.CONFIGURATION, "giveaway_dashboard_invalidchannel"))
        }
        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) { /* no permissions in channel */
            return ActionResult()
                    .withErrorMessage(getString(TextManager.GENERAL, "permission_channel_reactions", "#${channel.getName()}"))
        }

        if (config.item.isEmpty()) { /* invalid article */
            return ActionResult()
                    .withErrorMessage(getString(Category.CONFIGURATION, "giveaway_noitem"))
        }

        return null
    }

    private fun sendMessage(guild: Guild, channelId: Long): Long {
        val channel = guild.getChannelById(GuildMessageChannel::class.java, channelId)
        val eb = Giveaway.getMessageEmbed(guildEntity.locale, config)

        if (mode == Mode.OVERVIEW) {
            val message = channel!!.sendMessageEmbeds(eb.build()).complete()
            if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) {
                message.addReaction(config.emoji).queue()
            }
            return message.idLong
        } else {
            channel!!.editMessageEmbedsById(config.messageId, eb.build()).complete()
            return config.messageId
        }
    }

    private fun switchMode(newMode: Mode) {
        mode = newMode
        if (mode == Mode.OVERVIEW) {
            config = GiveawayEntity()
            previousItem = config.item
            config.guildId = atomicGuild.idLong
        }
    }

}