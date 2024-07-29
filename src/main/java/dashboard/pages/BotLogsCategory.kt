package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.informationcategory.BotLogsCommand
import core.TextManager
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardText
import dashboard.components.PageField
import dashboard.container.DashboardContainer
import dashboard.container.ExpandableContainer
import dashboard.container.HorizontalContainer
import dashboard.container.VerticalContainer
import modules.BotLogs
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.BotLogEntity.Companion.findAll
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.utils.TimeFormat
import java.lang.Integer.min
import java.util.*

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

    override fun retrievePageDescription(): String {
        return getString(Category.INFORMATION, "botlogs_delete")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        var entries: List<BotLogEntity>? = null
        if (entryIds.isEmpty()) {
            entries = findAll(entityManager, atomicGuild.idLong)
            entryIds = entries.map { it.id!! }
        }

        for (i in page * ENTRIES_PER_PAGE until min(entryIds.size, (page + 1) * ENTRIES_PER_PAGE)) {
            val botLogEntity: BotLogEntity? = if (entries != null) {
                entries[i]
            } else {
                null
            }
            mainContainer.add(generateBotLogSlot(entryIds[i], botLogEntity))
        }

        mainContainer.add(PageField(page, entryIds.size, ENTRIES_PER_PAGE) { page = it })
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

}