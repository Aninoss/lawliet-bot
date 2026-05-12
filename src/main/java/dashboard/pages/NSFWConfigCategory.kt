package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.NSFWConfigCommand
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardComboBox
import dashboard.component.DashboardSwitch
import dashboard.component.DashboardText
import dashboard.component.DashboardTitle
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.nsfwfilter.DBNSFWFilters
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "nsfw",
    userPermissions = [Permission.MANAGE_SERVER],
    commandAccessRequirements = [NSFWConfigCommand::class]
)
class NSFWConfigCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(NSFWConfigCommand::class.java, locale).title
    }

    override fun retrievePageDescription(): String {
        return Command.getCommandLanguage(NSFWConfigCommand::class.java, locale).descShort
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.add(
            generateGeneralField(),
            DashboardTitle(getString(Category.CONFIGURATION, "nsfwconfig_dashboard_filter_title")),
            DashboardText(getString(Category.CONFIGURATION, "nsfwconfig_dashboard_filter_subtext")),
            generateFilterField(guild)
        )
    }

    private fun generateGeneralField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val spoilersSwitch = DashboardSwitch(getString(Category.CONFIGURATION, "nsfwconfig_root_spoiler_label")) {
            guildEntity.beginTransaction()
            guildEntity.nsfwSpoilers = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.NSFW_SPOILERS, atomicMember, null, it.data)
            guildEntity.commitTransaction()
            ActionResult()
        }
        spoilersSwitch.subtitle = getString(Category.CONFIGURATION, "nsfwconfig_root_spoiler_subtext")
        spoilersSwitch.isChecked = guildEntity.nsfwSpoilers
        container.add(spoilersSwitch)
        return container
    }

    private fun generateFilterField(guild: Guild): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val nsfwKeywords = DBNSFWFilters.getInstance().retrieve(guild.idLong).keywords
        val label = getString(Category.CONFIGURATION, "nsfwconfig_dashboard_filter_label")
        val comboBox = DashboardComboBox(label, emptyList(), true, NSFWConfigCommand.MAX_FILTERS) {
            if (it.type == "add") {
                entityManager.transaction.begin()
                it.data.split(" ").forEach { data ->
                    if (data.length <= NSFWConfigCommand.MAX_LENGTH && !nsfwKeywords.contains(data) && data.length > 0) {
                        BotLogEntity.log(entityManager, BotLogEntity.Event.NSFW_FILTER, atomicMember, data.lowercase(), null)
                        nsfwKeywords.add(data.lowercase())
                    }
                }
                entityManager.transaction.commit()
            } else if (it.type == "remove") {
                entityManager.transaction.begin()
                BotLogEntity.log(entityManager, BotLogEntity.Event.NSFW_FILTER, atomicMember, null, it.data)
                entityManager.transaction.commit()

                nsfwKeywords.remove(it.data)
            }
            ActionResult()
                .withRedraw()
        }
        comboBox.allowCustomValues = true
        comboBox.selectedValues = nsfwKeywords.map { DiscordEntity(it, it) }
        comboBox.placeholder = getString(Category.CONFIGURATION, "nsfwfilter_dashboard_filter_placeholder")

        container.add(comboBox)
        return container
    }

}