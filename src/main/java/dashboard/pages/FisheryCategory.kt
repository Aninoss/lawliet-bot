package dashboard.pages

import commands.Category
import commands.runnables.fisherysettingscategory.FisheryCommand
import core.GlobalThreadPool
import core.TextManager
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import modules.fishery.FisheryStatus
import mysql.modules.fisheryusers.DBFishery
import mysql.modules.fisheryusers.FisheryGuildData
import mysql.modules.guild.GuildData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "fishery",
    userPermissions = [Permission.MANAGE_SERVER]
)
class FisheryCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    override fun retrievePageTitle(): String {
        return getString(TextManager.COMMANDS, "fishery_category")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val fisheryData = DBFishery.getInstance().retrieve(guild.idLong)
        val guildData = fisheryData.guildData
        mainContainer.add(
            generateStateField(guildData),
            generateStateButtons(guildData),
            DashboardSeparator(),
            generateSwitches(guildData),
            generateExcludeChannelsField(fisheryData)
        )
    }

    private fun generateExcludeChannelsField(fisheryData: FisheryGuildData): DashboardComponent {
        val container = VerticalContainer()
        container.add(
            HorizontalContainer(),
            DashboardTitle(getString(Category.FISHERY_SETTINGS, "fishery_state0_mchannels"))
        )
        val comboBox = DashboardTextChannelComboBox(
            "",
            fisheryData.guildId,
            fisheryData.ignoredChannelIds,
            true,
            FisheryCommand.MAX_CHANNELS
        )
        container.add(comboBox)
        return container
    }

    private fun generateSwitches(guildData: GuildData): DashboardComponent {
        val container = VerticalContainer()

        val switchTreasure = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mtreasurechests_title", "").trim()) {
            guildData.toggleFisheryTreasureChests()
            ActionResult(false)
        }
        switchTreasure.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mtreasurechests_desc")
        switchTreasure.isChecked = guildData.isFisheryTreasureChests
        container.add(switchTreasure)

        val switchReminders = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mreminders_title", "").trim()) {
            guildData.toggleFisheryReminders()
            ActionResult(false)
        }
        switchReminders.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mreminders_desc")
        switchReminders.isChecked = guildData.isFisheryReminders
        container.add(switchReminders)

        val switchCoinLimit = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mcoinsgivenlimit_title", "").trim()) {
            guildData.toggleFisheryCoinsGivenLimit()
            ActionResult(false)
        }
        switchCoinLimit.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mcoinsgivenlimit_desc")
        switchCoinLimit.isChecked = guildData.hasFisheryCoinsGivenLimit()
        container.add(switchCoinLimit)

        return container
    }

    private fun generateStateButtons(guildData: GuildData): DashboardComponent {
        val buttonsContainer = HorizontalContainer()
        buttonsContainer.allowWrap = true
        when (guildData.fisheryStatus!!) {
            FisheryStatus.ACTIVE -> {
                val pauseButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_pause")) {
                    guildData.fisheryStatus = FisheryStatus.PAUSED
                    ActionResult(true)
                }
                pauseButton.style = DashboardButton.Style.DEFAULT
                buttonsContainer.add(pauseButton)

                val stopButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_stop")) {
                    GlobalThreadPool.getExecutorService().submit { DBFishery.getInstance().invalidateGuildId(guildData.guildId) }
                    guildData.fisheryStatus = FisheryStatus.STOPPED
                    ActionResult(true)
                }
                stopButton.enableConfirmationMessage(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_danger"))
                stopButton.style = DashboardButton.Style.DANGER
                buttonsContainer.add(stopButton, HorizontalPusher())
            }
            FisheryStatus.PAUSED -> {
                val resumeButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_resume")) {
                    guildData.fisheryStatus = FisheryStatus.ACTIVE
                    ActionResult(true)
                }
                resumeButton.style = DashboardButton.Style.PRIMARY
                buttonsContainer.add(resumeButton, HorizontalPusher())
            }
            FisheryStatus.STOPPED -> {
                val startButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_start")) {
                    guildData.fisheryStatus = FisheryStatus.ACTIVE
                    ActionResult(true)
                }
                startButton.style = DashboardButton.Style.PRIMARY
                buttonsContainer.add(startButton, HorizontalPusher())
            }
        }

        return buttonsContainer
    }

    private fun generateStateField(guildData: GuildData): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val statusContainer = HorizontalContainer()
        val statusTextKey = getString(Category.FISHERY_SETTINGS, "fishery_state0_mstatus")
        val statusTextValue = getString(Category.FISHERY_SETTINGS, "fishery_state0_status").split("\n")[guildData.fisheryStatus.ordinal]
        statusContainer.add(
            DashboardText("$statusTextKey:"),
            DashboardText(statusTextValue),
            HorizontalPusher()
        )
        container.add(statusContainer)
        return container
    }

}