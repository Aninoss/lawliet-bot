package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.fisherysettingscategory.FisheryCommand
import commands.runnables.fisherysettingscategory.FisheryRolesCommand
import commands.runnables.fisherysettingscategory.VCTimeCommand
import constants.Emojis
import constants.Settings
import core.CustomObservableList
import core.GlobalThreadPool
import core.MemberCacheController
import core.TextManager
import core.utils.StringUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardMultiMembersComboBox
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.components.DashboardMultiTextChannelsComboBox
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import dashboard.data.GridRow
import modules.fishery.Fishery
import modules.fishery.FisheryGear
import modules.fishery.FisheryManage
import modules.fishery.FisheryStatus
import mysql.modules.fisheryusers.DBFishery
import mysql.modules.fisheryusers.FisheryGuildData
import mysql.modules.fisheryusers.FisheryMemberData
import mysql.modules.guild.GuildData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "fishery",
    userPermissions = [Permission.MANAGE_SERVER],
    botPermissions = [Permission.MANAGE_ROLES]
)
class FisheryCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    var manageMembers = CustomObservableList<Long>(emptyList())
    var manageRoles = CustomObservableList<Long>(emptyList())
    var managePropertyIndex = 0
    var manageNewValue = "+0"

    override fun retrievePageTitle(): String {
        return getString(TextManager.COMMANDS, "fishery_category")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        clearAttributes()
        val fisheryData = DBFishery.getInstance().retrieve(guild.idLong)
        val guildData = fisheryData.guildData
        val premium = isPremium

        mainContainer.add(
            generateStateField(guildData),
            generateStateButtons(guildData),
            DashboardSeparator(),
            generateSwitches(guildData),
            generateExcludeChannelsField(fisheryData),
            generateVoiceLimitField(guildData, premium),
            generateFisheryRolesField(guildData, fisheryData),
            generateFisheryRolesPreviewField(fisheryData),
            generateFisheryManageField(fisheryData, guildData, premium)
        )
    }

    private fun generateFisheryManageField(fisheryGuildData: FisheryGuildData, guildData: GuildData, premium: Boolean): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.FISHERY_SETTINGS, "fisherymanage_title")))

        if (guildData.fisheryStatus == FisheryStatus.ACTIVE) {
            container.add(
                generateFisheryManageMembersField(premium),
                DashboardSeparator(),
                generateFisheryManageActionField(fisheryGuildData, premium),
                DashboardText(getString(Category.FISHERY_SETTINGS, "fisherymanage_value_mask"))
            )

            val clearButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fisherymanage_state0_reset")) {
                if (manageMembers.size > 0 || manageRoles.size > 0) {
                    val fisheryMemberList = collectFisheryMemberList(fisheryGuildData)
                    fisheryMemberList.forEach(FisheryMemberData::remove)
                    ActionResult()
                        .withSuccessMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_reset_success", fisheryMemberList.size != 1, StringUtil.numToString(fisheryMemberList.size)))
                } else {
                    ActionResult()
                        .withErrorMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_nomembers"))
                }
            }
            clearButton.isEnabled = premium
            clearButton.style = DashboardButton.Style.DANGER
            clearButton.enableConfirmationMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_resetwarning"))
            container.add(DashboardSeparator(), HorizontalContainer(clearButton, HorizontalPusher()))
        } else {
            container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "fisherymanage_notactive")))
        }

        if (!premium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }
        return container
    }

    private fun generateFisheryManageActionField(fisheryGuildData: FisheryGuildData, premium: Boolean): DashboardComponent {
        val container = HorizontalContainer()
        container.allowWrap = true
        container.alignment = HorizontalContainer.Alignment.BOTTOM

        val properties = ArrayList<String>()
        getString(Category.FISHERY_SETTINGS, "fisherymanage_options").split('\n').forEach { properties += it }
        for (i in 0 until FisheryGear.values().size) {
            properties += getString(Category.FISHERY, "buy_product_${i}_0")
        }
        val values = properties.mapIndexed { i, name ->
            val emoji = when (i) {
                0 -> Emojis.FISH.formatted
                1 -> Emojis.COINS_UNICODE.formatted
                2 -> Emojis.DAILY_STREAK.formatted
                else -> FisheryGear.values()[i - 3].emoji
            }
            DiscordEntity(i.toString(), "$emoji $name")
        }
        val propertyComboBox = DashboardComboBox(getString(Category.FISHERY_SETTINGS, "fisherymanage_property"), values, false, 1) {
            managePropertyIndex = it.data.toInt()
            ActionResult()
        }
        propertyComboBox.selectedValues = listOf(values[managePropertyIndex])
        propertyComboBox.isEnabled = premium
        container.add(propertyComboBox)

        val valueTextField = DashboardTextField(getString(Category.FISHERY_SETTINGS, "fisherymanage_textfield"), 1, 30) {
            manageNewValue = it.data.replace(Regex("[<>]"), "")
            ActionResult()
        }
        valueTextField.value = manageNewValue
        valueTextField.editButton = false
        valueTextField.isEnabled = premium
        container.add(valueTextField)

        val confirmButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fisherymanage_confirm")) {
            if (manageMembers.size > 0 || manageRoles.size > 0) {
                val fisheryMemberList = collectFisheryMemberList(fisheryGuildData)
                if (FisheryManage.updateValues(fisheryMemberList, managePropertyIndex, manageNewValue)) {
                    ActionResult()
                        .withSuccessMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_modify_success", fisheryMemberList.size != 1, StringUtil.numToString(fisheryMemberList.size)))
                } else {
                    ActionResult()
                        .withErrorMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_invalidvalue"))
                }
            } else {
                ActionResult()
                    .withErrorMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_nomembers"))
            }
        }
        confirmButton.style = DashboardButton.Style.PRIMARY
        confirmButton.isEnabled = premium
        container.add(confirmButton)

        return container
    }

    private fun collectFisheryMemberList(fisheryGuildData: FisheryGuildData): List<FisheryMemberData> {
        val memberSet = HashSet<Long>()

        atomicGuild.get().orElse(null)?.let { guild ->
            MemberCacheController.getInstance().loadMembersFull(guild)
            memberSet.addAll(manageMembers)
            for (roleId in manageRoles) {
                guild.getRoleById(roleId)?.let { role ->
                    guild.getMembersWithRoles(role).forEach { memberSet += it.idLong }
                }
            }
        }

        return memberSet.map { fisheryGuildData.getMemberData(it) }
    }

    private fun generateFisheryManageMembersField(premium: Boolean): DashboardComponent {
        val container = VerticalContainer()

        val manageMembers = DashboardMultiMembersComboBox(
            getString(Category.FISHERY_SETTINGS, "fisherymanage_members"),
            atomicGuild.idLong,
            manageMembers,
            true,
            50
        )
        manageMembers.isEnabled = premium
        container.add(manageMembers)

        val manageRoles = DashboardMultiRolesComboBox(
            getString(Category.FISHERY_SETTINGS, "fisherymanage_roles"),
            locale,
            atomicGuild.idLong,
            atomicMember.idLong,
            manageRoles,
            true,
            50,
            false
        )
        manageRoles.isEnabled = premium
        container.add(manageRoles)
        return container
    }

    private fun generateFisheryRolesPreviewField(fisheryData: FisheryGuildData): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.FISHERY_SETTINGS, "fisheryroles_preview")))

        val roles = fisheryData.roles
        if (roles.size > 0) {
            val rows = roles.mapIndexed { n, role ->
                val values = arrayOf((n + 1).toString(), role.name, StringUtil.numToString(Fishery.getFisheryRolePrice(role.guild, roles.size, n)))
                GridRow(n.toString(), values)
            }
            val grid = DashboardGrid(
                getString(Category.FISHERY_SETTINGS, "fisheryroles_grid_title").split("\n").toTypedArray(),
                rows
            )
            container.add(grid)
        }

        val refreshButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fisheryroles_refresh")) {
            ActionResult()
                .withRedraw()
        }
        container.add(HorizontalContainer(refreshButton, HorizontalPusher()), DashboardText(getString(Category.FISHERY_SETTINGS, "fisheryroles_order")))
        return container
    }

    private fun generateFisheryRolesField(guildData: GuildData, fisheryData: FisheryGuildData): DashboardComponent {
        val container = VerticalContainer()
        container.add(
            DashboardTitle(Command.getCommandLanguage(FisheryRolesCommand::class.java, locale).title),
            DashboardText(getString(Category.FISHERY_SETTINGS, "fisheryroles_exp"))
        )

        val rolesComboBox = DashboardMultiRolesComboBox(
            getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_mroles"),
            locale,
            fisheryData.guildId,
            atomicMember.idLong,
            fisheryData.roleIds,
            true,
            FisheryRolesCommand.MAX_ROLES,
            true
        )
        container.add(rolesComboBox)

        val announcementChannelComboBox = DashboardTextChannelComboBox(
            getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_mannouncementchannel"),
            atomicGuild.idLong,
            guildData.fisheryAnnouncementChannelId.orElse(null),
            true
        ) {
            guildData.setFisheryAnnouncementChannelId(it.data?.toLong())
        }
        container.add(announcementChannelComboBox)
        container.add(DashboardSeparator(), generateFisheryRolePricesField(guildData))

        val singleRolesSwitch = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_msinglerole_raw")) {
            guildData.toggleFisherySingleRoles()
            ActionResult()
        }
        singleRolesSwitch.subtitle = getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_msinglerole_desc").replace("*", "")
        singleRolesSwitch.isChecked = guildData.isFisherySingleRoles
        container.add(DashboardSeparator(), singleRolesSwitch)
        return container;
    }

    private fun generateFisheryRolePricesField(guildData: GuildData): DashboardComponent {
        val container = HorizontalContainer()
        container.alignment = HorizontalContainer.Alignment.CENTER
        container.allowWrap = true
        container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_mroleprices") + ":"))

        val min = DashboardNumberField(getString(Category.FISHERY_SETTINGS, "fisheryroles_first"), 0, Settings.FISHERY_MAX) {
            guildData.setFisheryRolePrices(it.data.toLong(), guildData.fisheryRoleMax)
            ActionResult()
        }
        min.value = guildData.fisheryRoleMin
        container.add(min)

        val max = DashboardNumberField(getString(Category.FISHERY_SETTINGS, "fisheryroles_last"), 0, Settings.FISHERY_MAX) {
            guildData.setFisheryRolePrices(guildData.fisheryRoleMin, it.data.toLong())
            ActionResult()
        }
        max.value = guildData.fisheryRoleMax
        container.add(max)
        return container
    }

    private fun generateVoiceLimitField(guildData: GuildData, premium: Boolean): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(Command.getCommandLanguage(VCTimeCommand::class.java, locale).title))

        val horizontalContainer = HorizontalContainer()
        horizontalContainer.allowWrap = true
        horizontalContainer.alignment = HorizontalContainer.Alignment.BOTTOM

        val limitNumberField = DashboardNumberField(getString(Category.FISHERY_SETTINGS, "vctime_hoursperday"), 1, 24) {
            guildData.setFisheryVcHoursCap(it.data.toInt() % 24)
            ActionResult()
        }
        guildData.fisheryVcHoursCapEffectively.ifPresentOrElse({
            limitNumberField.value = it.toLong()
        }, {
            limitNumberField.value = 24
        })
        limitNumberField.isEnabled = premium
        horizontalContainer.add(limitNumberField)

        val unlimitedButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "vctime_setunlimited")) {
            guildData.setFisheryVcHoursCap(0)
            ActionResult()
                .withRedraw()
        }
        unlimitedButton.isEnabled = premium
        horizontalContainer.add(unlimitedButton)

        container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "vctime_explanation")), horizontalContainer)

        if (!premium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }

        return container
    }

    private fun generateExcludeChannelsField(fisheryData: FisheryGuildData): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.FISHERY_SETTINGS, "fishery_state0_mchannels")))
        val comboBox = DashboardMultiTextChannelsComboBox(
            fisheryData.guildId,
            fisheryData.ignoredChannelIds,
            true,
            FisheryCommand.MAX_CHANNELS
        )
        container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "fishery_excludedchannels")), comboBox)
        return container
    }

    private fun generateSwitches(guildData: GuildData): DashboardComponent {
        val container = VerticalContainer()

        val switchTreasure = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mtreasurechests_title", "").trim()) {
            guildData.toggleFisheryTreasureChests()
            ActionResult()
        }
        switchTreasure.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mtreasurechests_desc")
        switchTreasure.isChecked = guildData.isFisheryTreasureChests
        container.add(switchTreasure)

        val switchReminders = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mreminders_title", "").trim()) {
            guildData.toggleFisheryReminders()
            ActionResult()
        }
        switchReminders.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mreminders_desc")
        switchReminders.isChecked = guildData.isFisheryReminders
        container.add(switchReminders)

        val switchCoinLimit = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mcoinsgivenlimit_title", "").trim()) {
            guildData.toggleFisheryCoinsGivenLimit()
            ActionResult()
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
                    ActionResult()
                        .withRedraw()
                }
                pauseButton.style = DashboardButton.Style.DEFAULT
                buttonsContainer.add(pauseButton)

                val stopButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_stop")) {
                    GlobalThreadPool.getExecutorService().submit { DBFishery.getInstance().invalidateGuildId(guildData.guildId) }
                    guildData.fisheryStatus = FisheryStatus.STOPPED
                    ActionResult()
                        .withRedraw()
                }
                stopButton.enableConfirmationMessage(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_danger"))
                stopButton.style = DashboardButton.Style.DANGER
                buttonsContainer.add(stopButton, HorizontalPusher())
            }
            FisheryStatus.PAUSED -> {
                val resumeButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_resume")) {
                    guildData.fisheryStatus = FisheryStatus.ACTIVE
                    ActionResult()
                        .withRedraw()
                }
                resumeButton.style = DashboardButton.Style.PRIMARY
                buttonsContainer.add(resumeButton, HorizontalPusher())
            }
            FisheryStatus.STOPPED -> {
                val startButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_start")) {
                    guildData.fisheryStatus = FisheryStatus.ACTIVE
                    ActionResult()
                        .withRedraw()
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
        val statusTextValue = getString(Category.FISHERY_SETTINGS, "fishery_state0_status").split("\n")[guildData.fisheryStatus.ordinal].substring(2)
        statusContainer.add(
            DashboardText("$statusTextKey:"),
            DashboardText(statusTextValue)
        )
        container.add(statusContainer)
        return container
    }

    fun clearAttributes() {
        manageMembers = CustomObservableList<Long>(emptyList())
        manageRoles = CustomObservableList<Long>(emptyList())
        managePropertyIndex = 0
        manageNewValue = "+0"
    }

}