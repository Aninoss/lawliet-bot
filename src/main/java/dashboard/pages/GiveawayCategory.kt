package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.utilitycategory.GiveawayCommand
import core.CustomObservableMap
import core.ShardManager
import core.TextManager
import core.atomicassets.AtomicBaseGuildMessageChannel
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
import mysql.modules.giveaway.DBGiveaway
import mysql.modules.giveaway.GiveawayData
import mysql.modules.guild.DBGuild
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji
import java.time.Duration
import java.time.Instant
import java.util.*

@DashboardProperties(
    id = "giveaway",
    userPermissions = [Permission.MANAGE_SERVER],
)
class GiveawayCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    var channelId: Long? = null
    var article: String? = null
    var desc: String = ""
    var duration: Long = Duration.ofDays(7).toMinutes()
    var winners: Long = 1
    var emoji: Emoji = Emoji.fromUnicode("🎉")
    var image: String? = null

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(GiveawayCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val giveawayData = DBGiveaway.getInstance().retrieve(atomicGuild.idLong)
        mainContainer.add(
            generateOngoingGiveawaysTable(guild, giveawayData),
            generateCompletedGiveawaysTable(guild, giveawayData),
            generateGiveawayDataField(guild)
        )
    }

    private fun generateOngoingGiveawaysTable(guild: Guild, giveawayData: CustomObservableMap<Long, GiveawayData>): DashboardComponent {
        return generateGiveawaysTable(
            guild,
            giveawayData,
            getString(Category.UTILITY, "giveaway_dashboard_ongoing_title"),
            getString(Category.UTILITY, "giveaway_dashboard_ongoing_button"),
            { it.isActive }
        ) {
            //TODO
        }
    }

    private fun generateCompletedGiveawaysTable(guild: Guild, giveawayData: CustomObservableMap<Long, GiveawayData>): DashboardComponent {
        return generateGiveawaysTable(
            guild,
            giveawayData,
            getString(Category.UTILITY, "giveaway_dashboard_completed_title"),
            getString(Category.UTILITY, "giveaway_dashboard_completed_button"),
            { !it.isActive }
        ) {
            //TODO
        }
    }

    private fun generateGiveawaysTable(guild: Guild, giveawayData: CustomObservableMap<Long, GiveawayData>, title: String, rowButton: String,
                                       filter: (GiveawayData) -> Boolean, action: (DashboardEvent<String>) -> Any
    ): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(title))

        val rows = giveawayData.values
            .filter(filter)
            .map {
                val atomicChannel = AtomicBaseGuildMessageChannel(guild.idLong, it.baseGuildMessageChannelId)
                val values = arrayOf(it.title, atomicChannel.prefixedName)
                GridRow(it.hashCode().toString(), values)
            }

        val headers = getString(Category.UTILITY, "giveaway_dashboard_header").split('\n').toTypedArray()
        val grid = DashboardGrid(headers, rows) {
            action(it)
            ActionResult()
                .withRedraw()
        }
        grid.rowButton = rowButton
        container.add(grid)

        return container
    }

    private fun generateGiveawayDataField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()

        val title = getString(Category.UTILITY, "giveaway_state1_title")
        container.add(DashboardTitle(title))

        val channelArticleContainer = HorizontalContainer()
        channelArticleContainer.allowWrap = true

        val channelLabel = getString(Category.UTILITY, "giveaway_dashboard_channel")
        val channelComboBox = DashboardTextChannelComboBox(channelLabel, guild.idLong, channelId, false) {
            channelId = it.data.toLong()
            ActionResult()
        }
        channelArticleContainer.add(channelComboBox)

        val articleTextfield = DashboardTextField(getString(Category.UTILITY, "giveaway_state3_mtitle"), 1, GiveawayCommand.ARTICLE_LENGTH_MAX) {
            article = it.data
            ActionResult()
        }
        if (article != null) {
            articleTextfield.value = article
        }
        articleTextfield.editButton = false
        articleTextfield.placeholder = getString(Category.UTILITY, "giveaway_dashboard_article_placeholder")
        channelArticleContainer.add(articleTextfield)
        container.add(channelArticleContainer)

        val descTextfield =
            DashboardMultiLineTextField(getString(Category.UTILITY, "giveaway_state3_mdescription"), 0, GiveawayCommand.DESC_LENGTH_MAX) {
                desc = it.data
                ActionResult()
            }
        if (!desc.isEmpty()) {
            descTextfield.value = desc
        }
        descTextfield.editButton = false
        descTextfield.placeholder = getString(Category.UTILITY, "giveaway_dashboard_desc_placeholder")
        container.add(descTextfield, DashboardSeparator())

        val durationField = DashboardDurationField(getString(Category.UTILITY, "giveaway_state3_mduration")) {
            duration = it.data.toLong()
            ActionResult()
        }
        durationField.value = duration
        durationField.editButton = false
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
                        .withErrorMessage(getString(TextManager.GENERAL, "emojiunknown"))
                }
            }
        }
        emojiField.value = emoji.formatted
        winnersEmojiContainer.add(emojiField)
        container.add(winnersEmojiContainer)

        val imageUpload = DashboardImageUpload(getString(Category.UTILITY, "giveaway_dashboard_includedimage")) {
            image = it.data
            ActionResult()
        }
        container.add(imageUpload, DashboardSeparator())

        val buttonContainer = HorizontalContainer()
        val sendButton = DashboardButton(getString(Category.UTILITY, "giveaway_dashboard_send")) {
            val channel = channelId?.let { guild.getTextChannelById(it.toString()) }
            if (channel == null) { /* invalid channel */
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "giveaway_dashboard_invalidchannel"))
            }
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) { /* no permissions in channel */
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(TextManager.GENERAL, "permission_channel_reactions", "#${channel.getName()}"))
            }

            if (article == null) { /* invalid article */
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "giveaway_dashboard_noarticle"))
            }

            val instant = Instant.now()
            val messageId = sendMessage(guild, channelId!!, instant)
            val giveawayData = GiveawayData(guild.idLong, channelId!!, messageId, emoji.formatted, winners.toInt(), instant, duration, article, desc, image, true)
            GiveawayScheduler.loadGiveawayBean(giveawayData)
            DBGiveaway.getInstance().retrieve(guild.getIdLong())
                .put(messageId, giveawayData)

            ActionResult()
                .withRedrawScrollToTop()
        }
        sendButton.style = DashboardButton.Style.PRIMARY

        buttonContainer.add(sendButton, HorizontalPusher())
        container.add(buttonContainer)
        return container
    }

    private fun sendMessage(guild: Guild, channelId: Long, instant: Instant): Long {
        val channel = guild.getTextChannelById(channelId)
        val locale = DBGuild.getInstance().retrieve(guild.idLong).locale
        val eb = Giveaway.getMessageEmbed(locale, article, desc, winners.toInt(), emoji, duration, image, instant)
        val message = channel!!.sendMessageEmbeds(eb.build()).complete()
        if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) {
            message.addReaction(emoji).queue()
        }
        return message.idLong
    }

}