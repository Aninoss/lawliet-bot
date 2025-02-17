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
import core.featurelogger.FeatureLogger
import core.featurelogger.PremiumFeature
import core.utils.EmojiUtil
import core.utils.StringUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardChannelComboBox
import dashboard.components.DashboardMultiChannelsComboBox
import dashboard.components.DashboardMultiMembersComboBox
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import dashboard.data.GridRow
import modules.fishery.*
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.FisheryEntity
import mysql.hibernate.entity.guild.GuildEntity
import mysql.redis.fisheryusers.FisheryMemberData
import mysql.redis.fisheryusers.FisheryUserManager
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

    override fun retrievePageDescription(): String {
        return getString(Category.FISHERY_SETTINGS, "fishery_state0_description").replace("`", "\"")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        clearAttributes()

        if (anyCommandsAreAccessible(FisheryCommand::class)) {
            val innerContainer = VerticalContainer(
                    generateStateField(),
                    DashboardSeparator(true),
                    generateSwitches()
            )
            innerContainer.isCard = true
            mainContainer.add(innerContainer)

            mainContainer.add(
                    DashboardTitle(getString(Category.FISHERY_SETTINGS, "fishery_state0_mchannels")),
                    DashboardText(getString(Category.FISHERY_SETTINGS, "fishery_excludedchannels")),
                    generateExcludeChannelsField()
            )
        }

        if (anyCommandsAreAccessible(VCTimeCommand::class)) {
            mainContainer.add(
                    DashboardTitle(Command.getCommandLanguage(VCTimeCommand::class.java, locale).title),
                    DashboardText(getString(Category.FISHERY_SETTINGS, "vctime_explanation")),
                    generateVoiceLimitField()
            )
        }

        if (anyCommandsAreAccessible(FisheryRolesCommand::class)) {
            mainContainer.add(
                    DashboardTitle(Command.getCommandLanguage(FisheryRolesCommand::class.java, locale).title),
                    DashboardText(getString(Category.FISHERY_SETTINGS, "fisheryroles_exp")),
                    generateFisheryRolesField(),
                    generateFisheryRolesPreviewField()
            )
        }

        if (anyCommandsAreAccessible(FisheryManageCommand::class)) {
            mainContainer.add(
                    DashboardTitle(getString(Category.FISHERY_SETTINGS, "fisherymanage_title")),
                    DashboardText(getString(Category.FISHERY_SETTINGS, "fisherymanage_description")),
                    generateFisheryManageField()
            )
        }
    }

    private fun generateFisheryManageField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        if (fisheryEntity.fisheryStatus == FisheryStatus.ACTIVE) {
            container.add(
                    generateFisheryManageMembersField(isPremium),
                    DashboardSeparator(true),
                    generateFisheryManageActionField(isPremium),
                    DashboardText(getString(Category.FISHERY_SETTINGS, "fisherymanage_value_mask"), DashboardText.Style.HINT)
            )

            val clearButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fisherymanage_state0_reset")) {
                if (!anyCommandsAreAccessible(FisheryManageCommand::class)) {
                    return@DashboardButton ActionResult()
                            .withRedraw()
                }

                if (manageMembers.size > 0 || manageRoles.size > 0) {
                    FeatureLogger.inc(PremiumFeature.FISHERY_MANAGE, atomicGuild.idLong)

                    val fisheryMemberList = collectFisheryMemberList()
                    fisheryMemberList.forEach(FisheryMemberData::remove)

                    entityManager.transaction.begin()
                    BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_MANAGE_RESET, atomicMember, null, null, fisheryMemberList.map { it.memberId })
                    entityManager.transaction.commit()

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
            container.add(DashboardSeparator(true), HorizontalContainer(clearButton, HorizontalPusher()))
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
                0 -> EmojiUtil.getEmojiFromOverride(Emojis.FISH, "FISH").formatted
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
                val valueBefore = fisheryManageValue(fisheryMemberList)
                if (FisheryManage.updateValues(fisheryMemberList, guildEntity, managePropertyIndex, manageNewValue)) {
                    val valueAfter = fisheryManageValue(fisheryMemberList)

                    entityManager.transaction.begin()
                    BotLogEntity.log(entityManager, BotLogEntity.Event.values()[BotLogEntity.Event.FISHERY_MANAGE_FISH.ordinal + managePropertyIndex], atomicMember, valueBefore, valueAfter, fisheryMemberList.map { it.memberId })
                    entityManager.transaction.commit()

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

        return memberSet.map { FisheryUserManager.getGuildData(atomicGuild.idLong).getMemberData(it) }
    }

    private fun generateFisheryManageMembersField(premium: Boolean): DashboardComponent {
        val container = VerticalContainer()

        val manageMembers = DashboardMultiMembersComboBox(
                this,
                getString(Category.FISHERY_SETTINGS, "fisherymanage_members"),
                { manageMembers },
                true,
                50
        )
        manageMembers.isEnabled = premium
        container.add(manageMembers)

        val manageRolesComboBox = DashboardMultiRolesComboBox(
                this,
                getString(Category.FISHERY_SETTINGS, "fisherymanage_roles"),
                { manageRoles },
                true,
                50,
                false
        )
        manageRolesComboBox.isEnabled = premium
        container.add(manageRolesComboBox)
        return container
    }

    private fun generateFisheryRolesPreviewField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val roles = fisheryEntity.roles
        val rows = roles.mapIndexed { n, role ->
            val values = arrayOf(role.name, StringUtil.numToString(Fishery.getFisheryRolePrice(fisheryEntity.rolePriceMin, fisheryEntity.rolePriceMax, roles.size, n)))
            GridRow(n.toString(), values)
        }
        val grid = DashboardGrid(
                getString(Category.FISHERY_SETTINGS, "fisheryroles_grid_title").split("\n").toTypedArray(),
                rows
        )
        container.add(grid)

        val refreshButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "fisheryroles_refresh")) {
            ActionResult()
                    .withRedraw()
        }

        container.add(
                HorizontalContainer(refreshButton, HorizontalPusher()),
                DashboardText(getString(Category.FISHERY_SETTINGS, "fisheryroles_order"), DashboardText.Style.HINT)
        )
        return container
    }

    private fun generateFisheryRolesField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val rolesComboBox = DashboardMultiRolesComboBox(
                this,
                getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_mroles"),
                { it.fishery.roleIds },
                true,
                FisheryRolesCommand.MAX_ROLES,
                true,
                FisheryRolesCommand::class,
                BotLogEntity.Event.FISHERY_ROLES
        )
        container.add(rolesComboBox, DashboardText(getString(Category.FISHERY_SETTINGS, "fisheryroles_roleremovenote"), DashboardText.Style.WARNING))

        val announcementChannelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_mannouncementchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                fisheryEntity.roleUpgradeChannelId,
                true,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
        ) {
            if (!anyCommandsAreAccessible(FisheryRolesCommand::class)) {
                return@DashboardChannelComboBox ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_ROLES_UPGRADE_CHANNEL, atomicMember, fisheryEntity.roleUpgradeChannelId, it.data)
            fisheryEntity.roleUpgradeChannelId = it.data?.toLong()
            guildEntity.commitTransaction()

            ActionResult()
        }
        container.add(announcementChannelComboBox)
        container.add(DashboardSeparator(true), generateFisheryRolePricesField())

        val singleRolesSwitch = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_msinglerole_raw")) {
            if (!anyCommandsAreAccessible(FisheryRolesCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.singleRoles = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_ROLES_SINGLE_ROLES, atomicMember, null, fisheryEntity.singleRoles)
            guildEntity.commitTransaction()

            ActionResult()
        }
        singleRolesSwitch.subtitle = getString(Category.FISHERY_SETTINGS, "fisheryroles_state0_msinglerole_desc").replace("*", "")
        singleRolesSwitch.isChecked = fisheryEntity.singleRoles
        container.add(DashboardSeparator(true), singleRolesSwitch)
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
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_ROLES_PRICE_MIN, atomicMember, fisheryEntity.rolePriceMin, it.data)
            fisheryEntity.rolePriceMin = it.data.toLong()
            guildEntity.commitTransaction()

            ActionResult()
        }
        min.value = fisheryEntity.rolePriceMin
        container.add(min)

        val max = DashboardNumberField(getString(Category.FISHERY_SETTINGS, "fisheryroles_last"), 0, Settings.FISHERY_MAX) {
            if (!anyCommandsAreAccessible(FisheryRolesCommand::class)) {
                return@DashboardNumberField ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_ROLES_PRICE_MAX, atomicMember, fisheryEntity.rolePriceMax, it.data)
            fisheryEntity.rolePriceMax = it.data.toLong()
            guildEntity.commitTransaction()

            ActionResult()
        }
        max.value = fisheryEntity.rolePriceMax
        container.add(max)
        return container
    }

    private fun generateVoiceLimitField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val horizontalContainer = HorizontalContainer()
        horizontalContainer.allowWrap = true
        horizontalContainer.alignment = HorizontalContainer.Alignment.BOTTOM

        val limitNumberField = DashboardNumberField(getString(Category.FISHERY_SETTINGS, "vctime_hoursperday"), 1, 24) {
            if (!anyCommandsAreAccessible(VCTimeCommand::class)) {
                return@DashboardNumberField ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_VOICE_HOURS_LIMIT, atomicMember, fisheryEntity.voiceHoursLimit, it.data)
            fisheryEntity.voiceHoursLimit = it.data.toInt()
            guildEntity.commitTransaction()

            ActionResult()
        }
        limitNumberField.value = fisheryEntity.voiceHoursLimitEffectively.toLong()
        limitNumberField.isEnabled = isPremium
        horizontalContainer.add(limitNumberField)

        val unlimitedButton = DashboardButton(getString(Category.FISHERY_SETTINGS, "vctime_setunlimited")) {
            if (!anyCommandsAreAccessible(VCTimeCommand::class)) {
                return@DashboardButton ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_VOICE_HOURS_LIMIT, atomicMember, fisheryEntity.voiceHoursLimit, 24)
            fisheryEntity.voiceHoursLimit = 24
            guildEntity.commitTransaction()

            ActionResult()
                    .withRedraw()
        }
        unlimitedButton.isEnabled = isPremium
        unlimitedButton.setCanExpand(false)
        horizontalContainer.add(unlimitedButton)

        container.add(horizontalContainer)

        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }

        return container
    }

    private fun generateExcludeChannelsField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true
        val comboBox = DashboardMultiChannelsComboBox(
                this,
                "",
                DashboardComboBox.DataType.GUILD_CHANNELS,
                { it.fishery.excludedChannelIds },
                true,
                FisheryCommand.MAX_EXCLUDED_CHANNELS,
                FisheryCommand::class,
                BotLogEntity.Event.FISHERY_EXCLUDED_CHANNELS
        )
        container.add(comboBox)
        return container
    }

    private fun generateSwitches(): DashboardComponent {
        val container = VerticalContainer()

        val switchTreasure = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_treasurechests")) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.treasureChests = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_TREASURE_CHESTS, atomicMember, null, it.data)
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchTreasure.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mtreasurechests_desc")
        switchTreasure.isChecked = fisheryEntity.treasureChests
        container.add(switchTreasure)

        val switchPowerups = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_powerups")) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.powerUps = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_POWER_UPS, atomicMember, null, it.data)
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchPowerups.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mpowerups_desc")
        switchPowerups.isChecked = fisheryEntity.powerUps
        container.add(switchPowerups)

        val switchReminders = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_reminders")) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.fishReminders = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_FISH_REMINDERS, atomicMember, null, it.data)
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchReminders.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mreminders_desc")
        switchReminders.isChecked = fisheryEntity.fishReminders
        container.add(switchReminders)

        val switchCoinLimit = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_coingiftlimit")) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.coinGiftLimit = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_COIN_GIFT_LIMIT, atomicMember, null, it.data)
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchCoinLimit.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mcoinsgivenlimit_desc").replace("`", "\"")
        switchCoinLimit.isChecked = fisheryEntity.coinGiftLimit
        container.add(switchCoinLimit, DashboardSeparator(true))

        val switchAccountCards = DashboardSwitch(getString(Category.FISHERY_SETTINGS, "fishery_cards_dashboard")) {
            if (!anyCommandsAreAccessible(FisheryCommand::class)) {
                return@DashboardSwitch ActionResult()
                    .withRedraw()
            }

            guildEntity.beginTransaction()
            fisheryEntity.graphicallyGeneratedAccountCards = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_ACCOUNT_CARDS, atomicMember, null, it.data)
            guildEntity.commitTransaction()

            ActionResult()
        }
        switchAccountCards.isEnabled = isPremium
        switchAccountCards.subtitle = getString(Category.FISHERY_SETTINGS, "fishery_state0_mcards_desc")
        switchAccountCards.isChecked = fisheryEntity.graphicallyGeneratedAccountCardsEffectively
        container.add(switchAccountCards)

        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }
        container.add(DashboardSeparator(true))

        val probabilitiesContainer = HorizontalContainer()
        probabilitiesContainer.allowWrap = true

        val treasureChestProbability = DashboardFloatingNumberField(getString(Category.FISHERY_SETTINGS, "fishery_probabilities_treasure"), 0, 100) {
            fisheryEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_TREASURE_CHEST_PROBABILITY, atomicMember, fisheryEntity.treasureChestProbabilityInPercent, it.data)
            fisheryEntity.treasureChestProbabilityInPercent = it.data
            fisheryEntity.commitTransaction()

            return@DashboardFloatingNumberField ActionResult()
        }
        treasureChestProbability.value = fisheryEntity.treasureChestProbabilityInPercentEffectively
        treasureChestProbability.isEnabled = isPremium
        probabilitiesContainer.add(treasureChestProbability)

        val powerUpProbability = DashboardFloatingNumberField(getString(Category.FISHERY_SETTINGS, "fishery_probabilities_powerups"), 0, 100) {
            fisheryEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_POWER_UP_PROBABILITY, atomicMember, fisheryEntity.powerUpProbabilityInPercent, it.data)
            fisheryEntity.powerUpProbabilityInPercent = it.data
            fisheryEntity.commitTransaction()

            return@DashboardFloatingNumberField ActionResult()
        }
        powerUpProbability.value = fisheryEntity.powerUpProbabilityInPercentEffectively
        powerUpProbability.isEnabled = isPremium
        probabilitiesContainer.add(powerUpProbability)
        container.add(probabilitiesContainer)

        val workIntervalField = DashboardDurationField(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_workinterval")) {
            fisheryEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_WORK_INTERVAL, atomicMember, fisheryEntity.workIntervalMinutesEffectively, it.data)
            fisheryEntity.workIntervalMinutes = it.data
            fisheryEntity.commitTransaction()

            return@DashboardDurationField ActionResult()
        }
        workIntervalField.value = fisheryEntity.workIntervalMinutesEffectively
        workIntervalField.isEnabled = isPremium
        container.add(DashboardText(getString(Category.FISHERY_SETTINGS, "fishery_dashboard_workinterval")), workIntervalField)

        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }

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
                    BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_STATUS, atomicMember, fisheryEntity.fisheryStatus, FisheryStatus.PAUSED)
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

                    GlobalThreadPool.submit { FisheryUserManager.deleteGuildData(atomicGuild.idLong) }

                    guildEntity.beginTransaction()
                    BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_STATUS, atomicMember, fisheryEntity.fisheryStatus, FisheryStatus.STOPPED)
                    BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_DATA_RESET, atomicMember)
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
                    BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_STATUS, atomicMember, fisheryEntity.fisheryStatus, FisheryStatus.ACTIVE)
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
                    BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_STATUS, atomicMember, fisheryEntity.fisheryStatus, FisheryStatus.ACTIVE)
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
        val statusContainer = HorizontalContainer()
        statusContainer.allowWrap = true
        statusContainer.alignment = HorizontalContainer.Alignment.CENTER

        val statusTextValue = getString(Category.FISHERY_SETTINGS, "fishery_state0_status").split("\n")[fisheryEntity.fisheryStatus.ordinal].substring(2)
        statusContainer.add(
                DashboardText(statusTextValue),
                HorizontalPusher(),
                generateStateButtons()
        )
        return statusContainer
    }

    fun clearAttributes() {
        manageMembers = CustomObservableList<Long>(emptyList())
        manageRoles = CustomObservableList<Long>(emptyList())
        managePropertyIndex = 0
        manageNewValue = "+0"
    }

    fun fisheryManageValue(fisheryMemberList: List<FisheryMemberData>): String {
        val fisheryMemberGroup = FisheryMemberGroup(atomicGuild.idLong, fisheryMemberList)
        when (managePropertyIndex) {
            0 -> return fisheryMemberGroup.fishString
            1 -> return fisheryMemberGroup.coinsString
            2 -> return fisheryMemberGroup.dailyStreakString
            else -> return getString(Category.FISHERY_SETTINGS, "fisherymanage_gearlevel", fisheryMemberGroup.getGearString(FisheryGear.values()[managePropertyIndex - 3]))
        }
    }

}