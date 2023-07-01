package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.GiveawayCommand
import core.CustomObservableMap
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicStandardGuildMessageChannel
import core.utils.BotPermissionUtil
import core.utils.MentionUtil
import dashboard.*
import dashboard.component.*
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.GridRow
import modules.Giveaway
import modules.schedulers.GiveawayScheduler
import mysql.hibernate.entity.GuildEntity
import mysql.modules.giveaway.DBGiveaway
import mysql.modules.giveaway.GiveawayData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import java.time.Duration
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

    var channelId: Long? = null
    var article: String? = null
    var desc: String = ""
    var duration: Long = Duration.ofDays(7).toMinutes()
    var winners: Long = 1
    var emoji: Emoji = Emoji.fromUnicode("🎉")
    var image: String? = null
    var messageId: Long? = null
    var startInstant: Instant? = null

    var mode: Mode = Mode.OVERVIEW

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(GiveawayCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val giveawayDataMap = DBGiveaway.getInstance().retrieve(atomicGuild.idLong)

        if (mode == Mode.OVERVIEW) {
            mainContainer.add(
                generateOngoingGiveawaysTable(guild, giveawayDataMap),
                generateCompletedGiveawaysTable(guild, giveawayDataMap),
            )
        }

        mainContainer.add(generateGiveawayDataField(guild))
    }

    private fun generateOngoingGiveawaysTable(guild: Guild, giveawayDataMap: CustomObservableMap<Long, GiveawayData>): DashboardComponent {
        return generateGiveawaysTable(
            guild,
            giveawayDataMap,
            getString(Category.UTILITY, "giveaway_dashboard_ongoing_title"),
            getString(Category.UTILITY, "giveaway_dashboard_ongoing_button"),
            { it.isActive }
        ) {
            val giveawayDataTemp = DBGiveaway.getInstance().retrieve(atomicGuild.idLong).get(it.data.toLong())
            if (giveawayDataTemp != null && giveawayDataTemp.isActive) {
                readValuesFromGiveawayData(giveawayDataTemp)
                switchMode(Mode.EDIT)
            }
        }
    }

    private fun generateCompletedGiveawaysTable(guild: Guild, giveawayDataMap: CustomObservableMap<Long, GiveawayData>): DashboardComponent {
        return generateGiveawaysTable(
            guild,
            giveawayDataMap,
            getString(Category.UTILITY, "giveaway_dashboard_completed_title"),
            getString(Category.UTILITY, "giveaway_dashboard_completed_button"),
            { !it.isActive }
        ) {
            val giveawayDataTemp = DBGiveaway.getInstance().retrieve(atomicGuild.idLong).get(it.data.toLong())
            if (giveawayDataTemp != null && !giveawayDataTemp.isActive) {
                readValuesFromGiveawayData(giveawayDataTemp)
                switchMode(Mode.REROLL)
            }
        }
    }

    private fun generateGiveawaysTable(guild: Guild, giveawayDataMap: CustomObservableMap<Long, GiveawayData>, title: String, rowButton: String,
                                       filter: (GiveawayData) -> Boolean, action: (DashboardEvent<String>) -> Any
    ): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(title))

        val rows = giveawayDataMap.values
            .filter(filter)
            .map {
                val atomicChannel = AtomicStandardGuildMessageChannel(guild.idLong, it.standardGuildMessageChannelId)
                val values = arrayOf(it.title, atomicChannel.getPrefixedName(locale))
                GridRow(it.messageId.toString(), values)
            }

        val headers = getString(Category.UTILITY, "giveaway_dashboard_header").split('\n').toTypedArray()
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
            Mode.OVERVIEW -> getString(Category.UTILITY, "giveaway_state1_title")
            Mode.EDIT -> getString(Category.UTILITY, "giveaway_state2_title")
            Mode.REROLL -> getString(Category.UTILITY, "giveaway_state12_title")
        }
        container.add(DashboardTitle(title))

        val channelArticleContainer = HorizontalContainer()
        channelArticleContainer.allowWrap = true

        val channelLabel = getString(Category.UTILITY, "giveaway_dashboard_channel")
        val channelComboBox = DashboardTextChannelComboBox(channelLabel, locale, guild.idLong, channelId, false) {
            if (mode != Mode.OVERVIEW) {
                return@DashboardTextChannelComboBox ActionResult()
            }

            channelId = it.data.toLong()
            ActionResult()
        }
        channelComboBox.isEnabled = mode == Mode.OVERVIEW
        channelArticleContainer.add(channelComboBox)

        val articleTextfield = DashboardTextField(getString(Category.UTILITY, "giveaway_state3_mtitle"), 1, GiveawayCommand.ARTICLE_LENGTH_MAX) {
            if (mode == Mode.REROLL) {
                return@DashboardTextField ActionResult()
            }

            article = it.data
            ActionResult()
        }
        if (article != null) {
            articleTextfield.value = article
        }
        articleTextfield.editButton = false
        articleTextfield.placeholder = getString(Category.UTILITY, "giveaway_dashboard_article_placeholder")
        articleTextfield.isEnabled = mode != Mode.REROLL
        channelArticleContainer.add(articleTextfield)
        container.add(channelArticleContainer)

        val descTextfield =
            DashboardMultiLineTextField(getString(Category.UTILITY, "giveaway_state3_mdescription"), 0, GiveawayCommand.DESC_LENGTH_MAX) {
                if (mode == Mode.REROLL) {
                    return@DashboardMultiLineTextField ActionResult()
                }

                desc = it.data
                ActionResult()
            }
        if (!desc.isEmpty()) {
            descTextfield.value = desc
        }
        descTextfield.editButton = false
        descTextfield.placeholder = getString(Category.UTILITY, "giveaway_dashboard_desc_placeholder")
        descTextfield.isEnabled = mode != Mode.REROLL
        container.add(descTextfield, DashboardSeparator())

        val durationField = DashboardDurationField(getString(Category.UTILITY, "giveaway_state3_mduration")) {
            if (mode != Mode.OVERVIEW) {
                return@DashboardDurationField ActionResult()
            }

            duration = it.data.toLong()
            ActionResult()
        }
        durationField.value = duration
        durationField.editButton = false
        durationField.isEnabled = mode == Mode.OVERVIEW
        container.add(durationField)

        val winnersEmojiContainer = HorizontalContainer()
        winnersEmojiContainer.allowWrap = true

        val winnersField =
            DashboardNumberField(getString(Category.UTILITY, "giveaway_state3_mwinners"), GiveawayCommand.WINNERS_MIN.toLong(), GiveawayCommand.WINNERS_MAX.toLong()) {
                winners = it.data.toLong()
                ActionResult()
            }
        winnersField.value = winners
        winnersField.editButton = false
        winnersEmojiContainer.add(winnersField)

        val emojiField = DashboardTextField(getString(Category.UTILITY, "giveaway_state3_memoji"), 0, 100) {
            if (mode != Mode.OVERVIEW) {
                return@DashboardTextField ActionResult()
            }

            val emojis = MentionUtil.getEmojis(guild, it.data).list
            if (emojis.isEmpty()) {
                ActionResult()
                    .withRedraw()
                    .withErrorMessage(getString(Category.UTILITY, "giveaway_dashboard_noemoji"))
            } else {
                val emoji = emojis[0]
                if (emoji is UnicodeEmoji || ShardManager.customEmojiIsKnown(emoji as CustomEmoji)) {
                    this.emoji = emoji
                    ActionResult()
                        .withRedraw()
                } else {
                    ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(TextManager.GENERAL, "emojiunknown", emoji.name))
                }
            }
        }
        emojiField.value = emoji.formatted
        emojiField.isEnabled = mode == Mode.OVERVIEW
        winnersEmojiContainer.add(emojiField)
        container.add(winnersEmojiContainer, DashboardSeparator())

        if (mode != Mode.REROLL) {
            val imageUpload = DashboardImageUpload(getString(Category.UTILITY, "giveaway_dashboard_includedimage"), "giveaway") {
                image = it.data
                ActionResult()
                    .withRedraw()
            }
            container.add(imageUpload)

            if (image != null) {
                container.add(DashboardImage(image))
                val removeImageButton = DashboardButton(getString(Category.UTILITY, "giveaway_dashboard_removeimage")) {
                    image = null
                    ActionResult()
                        .withRedraw()
                }
                container.add(HorizontalContainer(removeImageButton, HorizontalPusher()))
            }
            container.add(DashboardSeparator())
        }

        val buttonContainer = HorizontalContainer()
        buttonContainer.allowWrap = true

        val sendButton = DashboardButton(getString(Category.UTILITY, "giveaway_dashboard_send", mode.ordinal)) {
            if (mode != Mode.REROLL) {
                confirm(guild, false)
            } else {
                val giveawayMap = DBGiveaway.getInstance().retrieve(guild.getIdLong())
                val giveawayData = giveawayMap.get(messageId)
                if (giveawayData == null) {
                    switchMode(Mode.OVERVIEW)
                    return@DashboardButton ActionResult()
                        .withRedrawScrollToTop()
                }

                val messageExists = GiveawayScheduler.processGiveawayUsers(giveawayData, winners.toInt(), true).get()
                if (messageExists) {
                    switchMode(Mode.OVERVIEW)
                    return@DashboardButton ActionResult()
                        .withRedrawScrollToTop()
                        .withSuccessMessage(getString(Category.UTILITY, "giveaway_dashboard_success", 2))
                } else {
                    return@DashboardButton ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(Category.UTILITY, "giveaway_nomessage"))
                }
            }
        }
        sendButton.style = DashboardButton.Style.PRIMARY
        buttonContainer.add(sendButton)

        if (mode == Mode.EDIT) {
            val endPrematurelyButton = DashboardButton(getString(Category.UTILITY, "giveaway_dashboard_endpre")) {
                confirm(guild, true)
            }
            endPrematurelyButton.style = DashboardButton.Style.PRIMARY
            buttonContainer.add(endPrematurelyButton)
        }

        if (mode == Mode.REROLL) {
            val removeButton = DashboardButton(getString(Category.UTILITY, "giveaway_state13_delete")) {
                DBGiveaway.getInstance().retrieve(guild.getIdLong()).remove(messageId)
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

    private fun confirm(guild: Guild, endPrematurely: Boolean): ActionResult {
        val errorActionResponse = checkFieldValidity(guild)
        if (errorActionResponse != null) {
            return errorActionResponse
        }

        val giveawayMap = DBGiveaway.getInstance().retrieve(guild.getIdLong())
        if (mode == Mode.EDIT && (!giveawayMap.containsKey(messageId) || !giveawayMap[messageId]!!.isActive)) {
            switchMode(Mode.OVERVIEW)
            return ActionResult()
                .withErrorMessage(getString(Category.UTILITY, "giveaway_dashboard_toolate"))
                .withRedrawScrollToTop()
        }

        if (mode == Mode.OVERVIEW) {
            startInstant = Instant.now()
        }

        if (endPrematurely) {
            duration = 0
        }

        try {
            messageId = sendMessage(guild, channelId!!, startInstant!!)
        } catch (e: ErrorResponseException) {
            if (mode != Mode.OVERVIEW) {
                return ActionResult()
                    .withRedraw()
                    .withErrorMessage(getString(Category.UTILITY, "giveaway_nomessage"))
            }
        }

        val giveawayData = generateGiveawayData(guild)
        val previousGiveawayData = giveawayMap.put(messageId, giveawayData);
        if (endPrematurely || previousGiveawayData == null) {
            GiveawayScheduler.loadGiveawayBean(giveawayData)
            if (endPrematurely) {
                TimeUnit.SECONDS.sleep(3)
            }
        }

        val actionResult = ActionResult()
            .withRedrawScrollToTop()
            .withSuccessMessage(getString(Category.UTILITY, "giveaway_dashboard_success", mode != Mode.OVERVIEW))
        switchMode(Mode.OVERVIEW)
        return actionResult
    }

    private fun checkFieldValidity(guild: Guild): ActionResult? {
        val channel = channelId?.let { guild.getTextChannelById(it.toString()) }
        if (channel == null) { /* invalid channel */
            return ActionResult()
                .withErrorMessage(getString(Category.UTILITY, "giveaway_dashboard_invalidchannel"))
        }
        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) { /* no permissions in channel */
            return ActionResult()
                .withErrorMessage(getString(TextManager.GENERAL, "permission_channel_reactions", "#${channel.getName()}"))
        }

        if (article == null || article!!.isBlank()) { /* invalid article */
            return ActionResult()
                .withErrorMessage(getString(Category.UTILITY, "giveaway_noitem"))
        }

        return null
    }

    private fun sendMessage(guild: Guild, channelId: Long, instant: Instant): Long {
        val channel = guild.getTextChannelById(channelId)
        val eb = Giveaway.getMessageEmbed(guildEntity.locale, article, desc, winners.toInt(), emoji, duration, image, instant)

        if (mode == Mode.OVERVIEW) {
            val message = channel!!.sendMessageEmbeds(eb.build()).complete()
            if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) {
                message.addReaction(emoji).queue()
            }
            return message.idLong
        } else {
            channel!!.editMessageEmbedsById(messageId!!, eb.build()).complete()
            return messageId!!
        }
    }

    private fun switchMode(newMode: Mode) {
        mode = newMode
        if (mode == Mode.OVERVIEW) {
            channelId = null
            article = null
            desc = ""
            duration = Duration.ofDays(7).toMinutes()
            winners = 1
            emoji = Emoji.fromUnicode("🎉")
            image = null
            messageId = null
            startInstant = null
        }
    }

    private fun readValuesFromGiveawayData(giveawayData: GiveawayData) {
        channelId = giveawayData.standardGuildMessageChannelId
        article = giveawayData.title
        desc = giveawayData.description
        duration = giveawayData.durationMinutes
        winners = giveawayData.winners.toLong()
        emoji = Emoji.fromFormatted(giveawayData.emoji)
        image = giveawayData.imageUrl.orElse(null)
        messageId = giveawayData.messageId
        startInstant = giveawayData.start
    }

    private fun generateGiveawayData(guild: Guild): GiveawayData {
        return GiveawayData(
            guild.idLong,
            channelId!!,
            messageId!!,
            emoji.formatted,
            winners.toInt(),
            startInstant,
            duration,
            article,
            desc,
            image,
            true
        )
    }

}