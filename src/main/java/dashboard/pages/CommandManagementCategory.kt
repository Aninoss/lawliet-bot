package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.CommandManagementCommand
import commands.runnables.configurationcategory.WhiteListCommand
import dashboard.DashboardCategory
import dashboard.DashboardProperties
import dashboard.component.DashboardText
import dashboard.component.DashboardTitle
import dashboard.components.DashboardMultipleTextChannelsComboBox
import dashboard.container.VerticalContainer
import mysql.modules.whitelistedchannels.DBWhiteListedChannels
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "commandmanagement",
    userPermissions = [Permission.MANAGE_SERVER]
)
class CommandManagementCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(CommandManagementCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val whitelistText = Command.getCommandLanguage(WhiteListCommand::class.java, locale).title
        mainContainer.add(
            DashboardTitle(whitelistText),
            DashboardText(getString(Category.CONFIGURATION, "whitelist_state0_description")),
            DashboardMultipleTextChannelsComboBox(guild.idLong, DBWhiteListedChannels.getInstance().retrieve(guild.idLong).channelIds, true, WhiteListCommand.MAX_CHANNELS)
        )
    }

}