package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.GiveawayCommand
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicGuildMessageChannel
import core.utils.BotPermissionUtil
import core.utils.MentionUtil
import dashboard.*
import dashboard.component.*
import dashboard.components.DashboardChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.GridRow
import modules.Giveaway
import modules.schedulers.GiveawayScheduler
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.GiveawayEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@DashboardProperties(
    id = "giveaway",
    userPermissions = [Permission.MANAGE_SERVER],
    commandAccessRequirements = [GiveawayCommand::class]
)
class GiveawayCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    enum class Mode { OVERVIEW, EDIT, REROLL }

    lateinit var config: GiveawayEntity
    var previousItem: String? = null
    var mode: Mode = Mode.OVERVIEW

    val giveawayEntities: MutableMap<Long, GiveawayEntity>
        get() = guildEntity.giveaways

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(GiveawayCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (previousItem == null) {
            switchMode(Mode.OVERVIEW)
        }

        if (mode == Mode.OVERVIEW) {
            mainContainer.add(
                generateOngoingGiveawaysTable(guild),
                generateCompletedGiveawaysTable(guild),
            )
        }

        mainContainer.add(generateGiveawayDataField(guild))
    }

    private fun generateOngoingGiveawaysTable(guild: Guild): DashboardComponent {
        return generateGiveawaysTable(
            guild,
            getString(Category.CONFIGURATION, "giveaway_dashboard_ongoing_title"),
            getString(Category.CONFIGURATION, "giveaway_dashboard_ongoing_button"),
            { it.active }
        ) {
            val giveaway = giveawayEntities.get(it.data.toLong())
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
            getString(Category.CONFIGURATION, "giveaway_dashboard_completed_title"),
            getString(Category.CONFIGURATION, "giveaway_dashboard_completed_button"),
            { !it.active }
        ) {
            val giveaway = giveawayEntities.get(it.data.toLong())
            if (giveaway != null && !giveaway.active) {
                config = giveaway.copy()
                previousItem = config.item
                switchMode(Mode.REROLL)
            }
        }
    }

    private fun generateGiveawaysTable(guild: Guild, title: String, rowButton: String,
                                       filter: (GiveawayEntity) -> Boolean, action: (DashboardEvent<String>) -> Any
    ): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(title))

        val rows = giveawayEntities.values
            .filter(filter)
            .map {
                val atomicChannel = AtomicGuildMessageChannel(guild.idLong, it.channelId)
                val values = arrayOf(it.item, atomicChannel.getPrefixedName(locale))
                GridRow(it.messageId.toString(), values)
            }

        val headers = getString(Category.CONFIGURATION, "giveaway_dashboard_header").split('\n').toTypedArray()
        val grid = DashboardGrid(headers, rows) {
            action(it)
            ActionResult()
                .withRedrawScrollToTop()
        }
        grid.rowButton = rowButton
        container.add(grid)

        return container
    }

    private fun generateGiveawayDataField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()

        val title = when (mode) {
            Mode.OVERVIEW -> getString(Category.CONFIGURATION, "giveaway_state1_title")
            Mode.EDIT -> getString(Category.CONFIGURATION, "giveaway_state2_title")
            Mode.REROLL -> getString(Category.CONFIGURATION, "giveaway_state12_title")
        }
        container.add(DashboardTitle(title))

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
        container.add(descTextfield, DashboardSeparator())

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

        val winnersEmojiContainer = HorizontalContainer()
        winnersEmojiContainer.allowWrap = true

        val winnersField =
            DashboardNumberField(getString(Category.CONFIGURATION, "giveaway_state3_mwinners"), GiveawayCommand.WINNERS_MIN.toLong(), GiveawayCommand.WINNERS_MAX.toLong()) {
                config.winners = it.data.toInt()
                ActionResult()
            }
        winnersField.value = config.winners.toLong()
        winnersField.editButton = false
        winnersEmojiContainer.add(winnersField)

        val emojiField = DashboardTextField(getString(Category.CONFIGURATION, "giveaway_state3_memoji"), 0, 100) {
            if (mode != Mode.OVERVIEW) {
                return@DashboardTextField ActionResult()
            }

            val emojis = MentionUtil.getEmojis(guild, it.data).list
            if (emojis.isEmpty()) {
                ActionResult()
                    .withRedraw()
                    .withErrorMessage(getString(Category.CONFIGURATION, "giveaway_dashboard_noemoji"))
            } else {
                val emoji = emojis[0]
                if (emoji is UnicodeEmoji || ShardManager.customEmojiIsKnown(emoji as CustomEmoji)) {
                    config.emoji = emoji
                    ActionResult()
                        .withRedraw()
                } else {
                    ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(TextManager.GENERAL, "emojiunknown", emoji.name))
                }
            }
        }
        emojiField.value = config.emojiFormatted
        emojiField.isEnabled = mode == Mode.OVERVIEW
        winnersEmojiContainer.add(emojiField)
        container.add(winnersEmojiContainer, DashboardSeparator())

        if (mode != Mode.REROLL) {
            val imageUpload = DashboardImageUpload(getString(Category.CONFIGURATION, "giveaway_dashboard_includedimage"), "giveaway", 1) {
                config.imageUrl = it.data
                ActionResult()
                    .withRedraw()
            }
            container.add(imageUpload)

            if (config.imageFilename != null) {
                container.add(DashboardImage(config.imageUrl))
                val removeImageButton = DashboardButton(getString(Category.CONFIGURATION, "giveaway_dashboard_removeimage")) {
                    config.imageFilename = null
                    ActionResult()
                        .withRedraw()
                }
                container.add(HorizontalContainer(removeImageButton, HorizontalPusher()))
            }
            container.add(DashboardSeparator())
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
                    return@DashboardButton ActionResult()
                        .withRedraw()
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
                return ActionResult()
                    .withRedraw()
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