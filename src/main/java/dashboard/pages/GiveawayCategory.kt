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
import mysql.modules.giveaway.DBGiveaway
import mysql.modules.giveaway.GiveawayData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji
import java.time.Duration
import java.util.*

@DashboardProperties(
    id = "giveaway",
    userPermissions = [Permission.MANAGE_SERVER],
)
class GiveawayCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    var channelId: Long? = null
    var article: String? = null
    var desc: String? = null
    var duration: Long = Duration.ofDays(7).toMinutes()
    var winners: Long = 1
    var emoji: Emoji = Emoji.fromUnicode("🎉")

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
        if (desc != null) {
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
        container.add(winnersEmojiContainer, DashboardSeparator())

        val buttonContainer = HorizontalContainer()

        val sendButton = DashboardButton(getString(Category.UTILITY, "giveaway_dashboard_send")) {
            val channel = channelId?.let { guild.getTextChannelById(it.toString()) }
            if (channel == null) { /* invalid channel */
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "giveaway_dashboard_invalidchannel"))
            }
            if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_ADD_REACTION)) { /* no permissions in channel */
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(TextManager.GENERAL, "permission_channel_reactions", "#${channel.getName()}"))
            }

            if (article == null) { /* invalid article */
                return@DashboardButton ActionResult()
                    .withErrorMessage(getString(Category.UTILITY, "giveaway_dashboard_noarticle"))
            }

            //TODO

            ActionResult()
                .withRedraw()
        }
        sendButton.style = DashboardButton.Style.PRIMARY

        buttonContainer.add(sendButton, HorizontalPusher())
        container.add(buttonContainer)
        return container
    }

}