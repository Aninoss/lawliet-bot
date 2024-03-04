package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.informationcategory.BotLogsCommand
import core.TextManager
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardButton
import dashboard.component.DashboardButton.Style
import dashboard.component.DashboardSeparator
import dashboard.component.DashboardText
import dashboard.container.*
import modules.BotLogs
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.BotLogEntity.Companion.findAll
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.utils.TimeFormat
import java.lang.Integer.min
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.max

@DashboardProperties(
        id = "botlogs",
        userPermissions = [Permission.VIEW_AUDIT_LOGS],
        commandAccessRequirements = [BotLogsCommand::class]
)
class BotLogsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    private val ENTRIES_PER_PAGE = 15

    private var entryIds: List<UUID> = emptyList()
    private var page = 0

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(BotLogsCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        var entries: List<BotLogEntity>? = null
        if (entryIds.isEmpty()) {
            entries = findAll(entityManager, atomicGuild.idLong)
            entryIds = entries.map { it.id!! }
        }

        mainContainer.add(DashboardText(getString(Category.INFORMATION, "botlogs_delete")))

        val disclaimerText = DashboardText(getString(Category.INFORMATION, "botlogs_disclaimer"))
        disclaimerText.style = DashboardText.Style.WARNING
        mainContainer.add(disclaimerText, DashboardSeparator())

        for (i in page * ENTRIES_PER_PAGE until min(entryIds.size, (page + 1) * ENTRIES_PER_PAGE)) {
            val botLogEntity: BotLogEntity? = if (entries != null) {
                entries[i]
            } else {
                null
            }
            mainContainer.add(generateBotLogSlot(entryIds[i], botLogEntity))
        }

        mainContainer.add(generatePagesFields())
    }

    private fun generateBotLogSlot(id: UUID, botLogPreload: BotLogEntity?): DashboardComponent {
        val botLog = botLogPreload ?: entityManager.find(BotLogEntity::class.java, id)
        if (botLog == null) {
            val deleted = DashboardText(getString(Category.INFORMATION, "botlogs_deleted"))
            return ExpandableContainer(VerticalContainer(deleted), VerticalContainer(deleted))
        }

        val timeString = StringBuilder(TimeFormat.DATE_TIME_SHORT.atInstant(botLog.timeCreate).toString())
        if (botLog.timeUpdate != null) {
            timeString.append(" - ")
                    .append(TimeFormat.DATE_TIME_SHORT.atInstant(botLog.timeUpdate!!).toString())
        }

        val desc = BotLogs.getMessage(locale, botLog, false)
        val header = DashboardText("$timeString｜$desc")
        return ExpandableContainer(VerticalContainer(header), generateBotLogValuesFields(botLog))
    }

    private fun generateBotLogValuesFields(botLog: BotLogEntity): DashboardContainer {
        val container = HorizontalContainer()
        container.allowWrap = true

        val fields = BotLogs.getExpandedValueFields(locale, atomicGuild.idLong, botLog, false)
        fields.forEach {
            val fieldContainer = VerticalContainer()

            val title = DashboardText(it.key)
            fieldContainer.add(title)

            val values = DashboardText(it.value)
            values.putCssProperty("margin-top", "0")

            fieldContainer.add(values)
            container.add(fieldContainer)
        }

        if (fields.isEmpty()) {
            val empty = DashboardText(getString(TextManager.GENERAL, "empty"))
            empty.style = DashboardText.Style.SECONDARY
            container.add(empty)
        }

        return container
    }

    private fun generatePagesFields(): DashboardComponent {
        val container = HorizontalContainer()
        val pageSize = (entryIds.size - 1) / ENTRIES_PER_PAGE + 1
        val pageNumbers = getVisiblePageNumbers(pageSize)

        pageNumbers.forEach { i ->
            val button: DashboardButton
            if (i != -1) {
                button = DashboardButton((i + 1).toString()) {
                    page = i
                    ActionResult()
                            .withRedraw()
                }
                button.isEnabled = page != i
            } else {
                button = DashboardButton("…")
                button.isEnabled = false
            }
            button.style = Style.TERTIARY
            container.add(button)
        }

        container.putCssProperty("margin-left", "0")
        return HorizontalContainer(HorizontalPusher(), container, HorizontalPusher())
    }

    private fun getVisiblePageNumbers(pageSize: Int): List<Int> {
        val pages: MutableList<Int>
        if (pageSize <= 7) {
            pages = IntStream.range(0, pageSize).boxed().collect(Collectors.toList<Int>())
        } else {
            pages = ArrayList()

            if (page <= 3) {
                pages.addAll(IntStream.rangeClosed(0, page).boxed().collect(Collectors.toList()))
            } else {
                pages.addAll(java.util.List.of(0, -1))
                val minPage = kotlin.math.min((page - 1).toDouble(), (pageSize - 5).toDouble()).toInt()
                for (i in minPage..page) {
                    pages.add(i)
                }
            }

            if (page >= pageSize - 4) {
                pages.addAll(IntStream.range(page + 1, pageSize).boxed().collect(Collectors.toList<Int>()))
            } else {
                val maxPage = max((page + 1).toDouble(), 4.0).toInt()
                for (i in page + 1..maxPage) {
                    pages.add(i)
                }
                pages.addAll(java.util.List.of<Int>(-1, pageSize - 1))
            }
        }

        return pages
    }

}