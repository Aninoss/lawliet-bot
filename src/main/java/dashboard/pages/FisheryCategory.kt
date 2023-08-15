package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.fisherysettingscategory.FisheryCommand
import commands.runnables.fisherysettingscategory.FisheryManageCommand
import commands.runnables.fisherysettingscategory.FisheryRolesCommand
import commands.runnables.fisherysettingscategory.VCTimeCommand
import constants.Emojis
import constants.Settings
import core.CustomObservableList
import core.GlobalThreadPool
import core.MemberCacheController
import core.TextManager
import core.utils.EmojiUtil
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
import mysql.hibernate.entity.FisheryEntity
import mysql.hibernate.entity.GuildEntity
import mysql.modules.fisheryusers.DBFishery
import mysql.modules.fisheryusers.FisheryMemberData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "fishery",
        userPermissions = [Permission.MANAGE_SERVER],
        botPermissions = [Permission.MANAGE_ROLES],
        commandAccessRequirements = [FisheryCommand::class, VCTimeCommand::class, FisheryRolesCommand::class, FisheryManageCommand::class]
)
class FisheryCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var manageMembers = CustomObservableList<Long>(emptyList())
    var manageRoles = CustomObservableList<Long>(emptyList())
    var managePropertyIndex = 0
    var manageNewValue = "+0"

    val fisheryEntity: FisheryEntity
        get() = guildEntity.fishery

    override fun retrievePageTitle(): String {
        return getString(TextManager.COMMANDS, "fishery_category")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        clearAttributes()

        if (anyCommandsAreAccessible(FisheryCommand::class)) {
            mainContainer.add(
                    generateStateField(),
                    generateStateButtons(),
                    DashboardSeparator(),
                    generateSwitches(),
                    generateExcludeChannelsField()
            )
        }

        if (anyCommandsAreAccessible(VCTimeCommand::class)) {
            mainContainer.add(
                    generateVoiceLimitField()
            )
        }

        if (anyCommandsAreAccessible(FisheryRolesCommand::class)) {
            mainContainer.add(
                    generateFisheryRolesField(),
                    generateFisheryRolesPreviewField()
            )
        }

        if (anyCommandsAreAccessible(FisheryManageCommand::class)) {
            mainContainer.add(
                    generateFisheryManageField()
            )
        }
    }

    private fun generateFisheryManageField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.FISHERY_SETTINGS, "fisherymanage_title")))

        if (fisheryEntity.fisheryStatus == FisheryStatus.ACTIVE) {
            container.add(
                    generateFisheryManageMembersField(isPremium),
                    DashboardSeparator(),
                    generateFisheryManageActionField(isPremium),
                    DashboardText(getString(Category.FISHERY_SETTINGS, "fisherymanage_value_mask"))
            )

            val clearButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fisherymanage_state0_reset")) {
                if (!anyCommandsAreAccessible(FisheryManageCommand::class)) {
                    return@DashboardButton ActionResult()
                            .withRedraw()
                }

                if (manageMembers.size > 0 || manageRoles.size > 0) {
                    val fisheryMemberList = collectFisheryMemberList()
                    fisheryMemberList.forEach(FisheryMemberData::remove)
                    ActionResult()
                            .withSuccessMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_reset_success", fisheryMemberList.size != 1, StringUtil.numToString(fisheryMemberList.size)))
                } else {
                    ActionResult()
                            .withErrorMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_nomembers"))
                }
            }
            clearButton.isEnabled = isPremium
            clearButton.style = DashboardButton.Style.DANGER
            clearButton.enableConfirmationMessage(getString(Category.FISHERY_SETTINGS, "fisherymanage_resetwarning"))
            container.add(DashboardSeparator(), HorizontalContainer(clearButton, HorizontalPusher()))
        } else {
            container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "fisherymanage_notactive")))
        }

        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }
        return container
    }

    private fun generateFisheryManageActionField(premium: Boolean): DashboardComponent {
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
                0 -> EmojiUtil.getUnicodeEmojiFromOverride(Emojis.FISH, "FISH").formatted
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
            if (!anyCommandsAreAccessible(FisheryManageCommand::class)) {
                return@DashboardButton ActionResult()
                        .withRedraw()
            }

            if (manageMembers.size > 0 || manageRoles.size > 0) {
                val fisheryMemberList = collectFisheryMemberList()
                if (FisheryManage.updateValues(fisheryMemberList, guildEntity, managePropertyIndex, manageNewValue)) {
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
        confirmButton.setCanExpand(false)
        container.add(confirmButton)

        return container
    }

    private fun collectFisheryMemberList(): List<FisheryMemberData> {
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

        return memberSet.map { DBFishery.getInstance().retrieve(atomicGuild.idLong).getMemberData(it) }
    }

    private fun generateFisheryManageMembersField(premium: Boolean): DashboardComponent {
        val container = VerticalContainer()

        val manageMembers = DashboardMultiMembersComboBox(
                getString(Category.FISHERY_SETTINGS, "fisherymanage_members"),
                locale,
                atomicGuild.idLong,
                manageMembers,
                true,
                50
        )
        manageMembers.isEnabled = premium
        container.add(manageMembers)

        val manageRoles = DashboardMultiRolesComboBox(
                this,
                getString(Category.FISHERY_SETTINGS, "fisherymanage_roles"),
                { manageRoles },
                true,
                50,
                false
        )
        manageRoles.isEnabled = premium
        container.add(manageRoles)
        return container
    }

    private fun generateFisheryRolesPreviewField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.FISHERY_SETTINGS, "fisheryroles_preview")))

        val roles = fisheryEntity.roles
        if (roles.isNotEmpty()) {
            val rows = roles.mapIndexed { n, role ->
                val values = arrayOf((n + 1).toString(), role.name, StringUtil.numToString(Fishery.getFisheryRolePrice(fisheryEntity.rolePriceMin!!, fisheryEntity.rolePriceMax!!, roles.size, n)))
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

    private fun generateFisheryRolesField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(
                DashboardTitle(Command.getCommandLanguage(FisheryRolesCommand::class.java, locale).title),
                DashboardText(getString(Category.FISHERY_SETTINGS, "fisheryroles_exp"))
        )

        val rolesComboBox = DashboardMultiRolesComboBox(
                this,
                getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_mroles"),
                { it.fishery.roleIds },
                true,
                FisheryRolesCommand.MAX_ROLES,
                true,
                FisheryRolesCommand::class
        )
        container.add(rolesComboBox)

        val announcementChannelComboBox = DashboardTextChannelComboBox(
                getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_mannouncementchannel"),
                locale,
                atomicGuild.idLong,
                fisheryEntity.roleUpgradeChannelId,
                true
        ) {
            if (!anyCommandsAreAccessible(FisheryRolesCommand::class)) {
                return@DashboardTextChannelComboBox ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.roleUpgradeChannelId = it.data?.toLong()
            guildEntity.commitTransaction()

            ActionResult()
        }
        container.add(announcementChannelComboBox)
        container.add(DashboardSeparator(), generateFisheryRolePricesField())

        val singleRolesSwitch = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_msinglerole_raw")) {
            if (!anyCommandsAreAccessible(FisheryRolesCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.singleRoles = it.data
            guildEntity.commitTransaction()

            ActionResult()
        }
        singleRolesSwitch.subtitle = getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_msinglerole_desc").replace("*", "")
        singleRolesSwitch.isChecked = fisheryEntity.singleRoles!!
        container.add(DashboardSeparator(), singleRolesSwitch)
        return container
    }

    private fun generateFisheryRolePricesField(): DashboardComponent {
        val container = HorizontalContainer()
        container.alignment = HorizontalContainer.Alignment.CENTER
        container.allowWrap = true
        container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_mroleprices") + ":"))

        val min = DashboardNumberField(getString(Category.FISHERY_SETTINGS, "fisheryroles_first"), 0, Settings.FISHERY_MAX) {
            if (!anyCommandsAreAccessible(FisheryRolesCommand::class)) {
                return@DashboardNumberField ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.rolePriceMin = it.data.toLong()
            guildEntity.commitTransaction()

            ActionResult()
        }
        min.value = fisheryEntity.rolePriceMin!!
        container.add(min)

        val max = DashboardNumberField(getString(Category.FISHERY_SETTINGS, "fisheryroles_last"), 0, Settings.FISHERY_MAX) {
            if (!anyCommandsAreAccessible(FisheryRolesCommand::class)) {
                return@DashboardNumberField ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.rolePriceMax = it.data.toLong()
            guildEntity.commitTransaction()

            ActionResult()
        }
        max.value = fisheryEntity.rolePriceMax!!
        container.add(max)
        return container
    }

    private fun generateVoiceLimitField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(Command.getCommandLanguage(VCTimeCommand::class.java, locale).title))

        val horizontalContainer = HorizontalContainer()
        horizontalContainer.allowWrap = true
        horizontalContainer.alignment = HorizontalContainer.Alignment.BOTTOM

        val limitNumberField = DashboardNumberField(getString(Category.FISHERY_SETTINGS, "vctime_hoursperday"), 1, 24) {
            if (!anyCommandsAreAccessible(VCTimeCommand::class)) {
                return@DashboardNumberField ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.voiceHoursLimit = if (it.data.toInt() < 24) {
                it.data.toInt()
            } else {
                null
            }
            guildEntity.commitTransaction()

            ActionResult()
        }
        limitNumberField.value = fisheryEntity.voiceHoursLimitEffectively?.toLong() ?: 24
        limitNumberField.isEnabled = isPremium
        horizontalContainer.add(limitNumberField)

        val unlimitedButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "vctime_setunlimited")) {
            if (!anyCommandsAreAccessible(VCTimeCommand::class)) {
                return@DashboardButton ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.voiceHoursLimit = null
            guildEntity.commitTransaction()

            ActionResult()
                    .withRedraw()
        }
        unlimitedButton.isEnabled = isPremium
        unlimitedButton.setCanExpand(false)
        horizontalContainer.add(unlimitedButton)

        container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "vctime_explanation")), horizontalContainer)

        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }

        return container
    }

    private fun generateExcludeChannelsField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.FISHERY_SETTINGS, "fishery_state0_mchannels")))
        val comboBox = DashboardMultiTextChannelsComboBox(
                this,
                "",
                { it.fishery.excludedChannelIds },
                true,
                FisheryCommand.MAX_CHANNELS,
                atomicMember.idLong,
                FisheryCommand::class
        )
        container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "fishery_excludedchannels")), comboBox)
        return container
    }

    private fun generateSwitches(): DashboardComponent {
        val container = VerticalContainer()

        val switchTreasure = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mtreasurechests_title", "").trim()) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.treasureChests = it.data
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchTreasure.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mtreasurechests_desc")
        switchTreasure.isChecked = fisheryEntity.treasureChests!!
        container.add(switchTreasure)

        val switchPowerups = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mpowerups_title", "").trim()) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.powerUps = it.data
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchPowerups.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mpowerups_desc")
        switchPowerups.isChecked = fisheryEntity.powerUps!!
        container.add(switchPowerups)

        val switchReminders = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mreminders_title", "").trim()) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.fishReminders = it.data
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchReminders.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mreminders_desc")
        switchReminders.isChecked = fisheryEntity.fishReminders!!
        container.add(switchReminders)

        val switchCoinLimit = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_state0_mcoinsgivenlimit_title", "").trim()) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.coinGiftLimit = it.data
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchCoinLimit.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mcoinsgivenlimit_desc")
        switchCoinLimit.isChecked = fisheryEntity.coinGiftLimit!!
        container.add(switchCoinLimit)

        return container
    }

    private fun generateStateButtons(): DashboardComponent {
        val buttonsContainer = HorizontalContainer()
        buttonsContainer.allowWrap = true
        when (fisheryEntity.fisheryStatus) {
            FisheryStatus.ACTIVE -> {
                val pauseButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_pause")) {
                    if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                        return@DashboardButton ActionResult()
                                .withRedraw()
                    }

                    guildEntity.beginTransaction()
                    fisheryEntity.fisheryStatus = FisheryStatus.PAUSED
                    guildEntity.commitTransaction()

                    ActionResult()
                            .withRedraw()
                }
                pauseButton.style = DashboardButton.Style.DEFAULT
                buttonsContainer.add(pauseButton)

                val stopButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_stop")) {
                    if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                        return@DashboardButton ActionResult()
                                .withRedraw()
                    }

                    GlobalThreadPool.submit { DBFishery.getInstance().invalidateGuildId(atomicGuild.idLong) }

                    guildEntity.beginTransaction()
                    fisheryEntity.fisheryStatus = FisheryStatus.STOPPED
                    guildEntity.commitTransaction()

                    ActionResult()
                            .withRedraw()
                }
                stopButton.enableConfirmationMessage(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_danger"))
                stopButton.style = DashboardButton.Style.DANGER
                buttonsContainer.add(stopButton, HorizontalPusher())
            }

            FisheryStatus.PAUSED -> {
                val resumeButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_resume")) {
                    if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                        return@DashboardButton ActionResult()
                                .withRedraw()
                    }

                    guildEntity.beginTransaction()
                    fisheryEntity.fisheryStatus = FisheryStatus.ACTIVE
                    guildEntity.commitTransaction()

                    ActionResult()
                            .withRedraw()
                }
                resumeButton.style = DashboardButton.Style.PRIMARY
                buttonsContainer.add(resumeButton, HorizontalPusher())
            }

            FisheryStatus.STOPPED -> {
                val startButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fishery_state0_button_start")) {
                    if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                        return@DashboardButton ActionResult()
                                .withRedraw()
                    }

                    guildEntity.beginTransaction()
                    fisheryEntity.fisheryStatus = FisheryStatus.ACTIVE
                    guildEntity.commitTransaction()

                    ActionResult()
                            .withRedraw()
                }
                startButton.style = DashboardButton.Style.PRIMARY
                buttonsContainer.add(startButton, HorizontalPusher())
            }
        }

        return buttonsContainer
    }

    private fun generateStateField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val statusContainer = HorizontalContainer()
        val statusTextKey = getString(Category.FISHERY_SETTINGS, "fishery_state0_mstatus")
        val statusTextValue = getString(Category.FISHERY_SETTINGS, "fishery_state0_status").split("\n")[fisheryEntity.fisheryStatus!!.ordinal].substring(2)
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