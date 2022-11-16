package dashboard.pages

import commands.Category
import commands.runnables.moderationcategory.InviteFilterCommand
import commands.runnables.moderationcategory.ModSettingsCommand
import commands.runnables.moderationcategory.WordFilterCommand
import core.TextManager
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
import modules.automod.WordFilter
import mysql.modules.bannedwords.BannedWordsData
import mysql.modules.bannedwords.DBBannedWords
import mysql.modules.moderation.DBModeration
import mysql.modules.moderation.ModerationData
import mysql.modules.spblock.DBSPBlock
import mysql.modules.spblock.SPBlockData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "moderation",
    userPermissions = [Permission.MANAGE_SERVER, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS],
    botPermissions = [Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS, Permission.MODERATE_MEMBERS],
    commandAccessRequirements = [ModSettingsCommand::class, InviteFilterCommand::class, WordFilterCommand::class]
)
class ModerationCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    var autoModConfigSlot: AutoModSlots? = null
    var autoModConfigStep = 0
    var autoModConfigTempValue = 1
    var autoModConfigTempDays = 1
    var autoModConfigTempDuration = 1

    override fun retrievePageTitle(): String {
        return getString(TextManager.COMMANDS, "moderation")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (autoModConfigSlot == null) {
            if (anyCommandsAreAccessible(ModSettingsCommand::class)) {
                mainContainer.add(
                    generateGeneralConfigurationField(),
                    generateAutoModField()
                )
            }

            if (anyCommandsAreAccessible(InviteFilterCommand::class)) {
                mainContainer.add(
                    generateInviteFilterField()
                )
            }

            if (anyCommandsAreAccessible(WordFilterCommand::class)) {
                mainContainer.add(
                    generateWordFilterField()
                )
            }
        } else {
            mainContainer.add(
                generateAutoModConfigTitle(),
                generateAutoModConfigText(),
                generateAutoModConfigTextField(),
                generateAutoModConfigButtons()
            )
        }
    }

    fun generateGeneralConfigurationField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(
            generateNotificationChannelComponent(),
            DashboardSeparator(),
            generateConfirmationMessageComponent(),
            DashboardSeparator(),
            generateJailRolesComponent()
        )
        return container
    }

    fun generateNotificationChannelComponent(): DashboardComponent {
        val channelComboBox = DashboardTextChannelComboBox(
            getString(Category.MODERATION, "mod_state0_mchannel"),
            atomicGuild.idLong,
            DBModeration.getInstance().retrieve(atomicGuild.idLong).announcementChannelId.orElse(null),
            true
        ) {
            if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                return@DashboardTextChannelComboBox ActionResult()
                    .withRedraw()
            }

            DBModeration.getInstance().retrieve(atomicGuild.idLong).setAnnouncementChannelId(it.data?.toLong())
            ActionResult()
        }
        return channelComboBox;
    }

    fun generateConfirmationMessageComponent(): DashboardComponent {
        val switch = DashboardSwitch(getString(Category.MODERATION, "mod_state0_mquestion")) {
            if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                return@DashboardSwitch ActionResult()
                    .withRedraw()
            }

            DBModeration.getInstance().retrieve(atomicGuild.idLong).question = it.data
            ActionResult()
        }
        switch.isChecked = DBModeration.getInstance().retrieve(atomicGuild.idLong).question
        return switch
    }

    fun generateJailRolesComponent(): DashboardComponent {
        return DashboardMultiRolesComboBox(
            getString(Category.MODERATION, "mod_state0_mjailroles"),
            locale,
            atomicGuild.idLong,
            atomicMember.idLong,
            DBModeration.getInstance().retrieve(atomicGuild.idLong).jailRoleIds,
            true,
            ModSettingsCommand.MAX_JAIL_ROLES,
            true,
            ModSettingsCommand::class
        )
    }

    fun generateAutoModField(): DashboardComponent {
        val modData = DBModeration.getInstance().retrieve(atomicGuild.idLong)
        val container = VerticalContainer()
        container.add(
            DashboardTitle(getString(Category.MODERATION, "mod_state0_mautomod")),
            generateAutoModSlotField(modData, AutoModSlots.MUTE),
            DashboardSeparator(),
            generateAutoModSlotField(modData, AutoModSlots.JAIL),
            DashboardSeparator(),
            generateAutoModSlotField(modData, AutoModSlots.KICK),
            DashboardSeparator(),
            generateAutoModSlotField(modData, AutoModSlots.BAN)
        )
        return container
    }

    fun generateAutoModSlotField(modData: ModerationData, slot: AutoModSlots): DashboardComponent {
        val value = slot.getValue(modData)
        val days = slot.getDays(modData)
        val duration = slot.getDuration(modData)

        val container = HorizontalContainer()
        container.allowWrap = true
        container.alignment = HorizontalContainer.Alignment.CENTER

        val name = getString(Category.MODERATION, "mod_auto${slot.id}")
        val status = if (value <= 0) {
            getString(Category.MODERATION, "mod_off")
        } else {
            ModSettingsCommand.getAutoModString(locale, value, days, duration).replace("*", "")
        }
        container.add(DashboardText("$name: $status"), HorizontalPusher())

        val buttonContainer = HorizontalContainer()

        val configButton = DashboardButton(getString(Category.MODERATION, "mod_config")) {
            if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                return@DashboardButton ActionResult()
                    .withRedraw()
            }

            autoModConfigSlot = slot
            autoModConfigStep = 0
            autoModConfigTempValue = 1
            autoModConfigTempDays = 1
            autoModConfigTempDuration = 1
            ActionResult()
                .withRedrawScrollToTop()
        }
        configButton.style = DashboardButton.Style.PRIMARY
        buttonContainer.add(configButton)

        if (slot.getValue(modData) > 0) {
            val turnOffButton = DashboardButton(getString(Category.MODERATION, "mod_state${slot.states[0]}_options")) {
                if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                    return@DashboardButton ActionResult()
                        .withRedraw()
                }

                slot.setData(modData, 0, 0, 0)
                ActionResult()
                    .withRedraw()
                    .withSuccessMessage(getString(Category.MODERATION, "mod_auto${slot.id}set"))
            }
            turnOffButton.style = DashboardButton.Style.DANGER
            buttonContainer.add(turnOffButton)
        }

        container.add(buttonContainer)

        return container
    }

    private fun generateInviteFilterField(): DashboardComponent {
        val inviteFilter = DBSPBlock.getInstance().retrieve(atomicGuild.idLong)
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.MODERATION, "invitefilter_title")))

        val activeSwitch = DashboardSwitch(getString(Category.MODERATION, "invitefilter_state0_menabled")) {
            if (!anyCommandsAreAccessible(InviteFilterCommand::class)) {
                return@DashboardSwitch ActionResult()
                    .withRedraw()
            }

            inviteFilter.isActive = it.data
            ActionResult()
                .withRedraw()
        }
        activeSwitch.isChecked = inviteFilter.isActive
        container.add(activeSwitch, DashboardSeparator(), generateInviteFilterExcludedField(inviteFilter), DashboardSeparator())

        val logReceivers = DashboardMultiMembersComboBox(
            getString(Category.MODERATION, "invitefilter_state0_mlogreciever"),
            atomicGuild.idLong,
            inviteFilter.logReceiverUserIds,
            true,
            InviteFilterCommand.MAX_LOG_RECEIVERS,
            atomicMember.idLong,
            InviteFilterCommand::class
        )
        container.add(logReceivers, DashboardSeparator())

        val actions = (0 until 3).map {
            DiscordEntity(it.toString(), getString(Category.MODERATION, "invitefilter_state0_mactionlist").split("\n")[it])
        }
        val action = DashboardComboBox(getString(Category.MODERATION, "invitefilter_state0_maction"), actions, false, 1) {
            if (!anyCommandsAreAccessible(InviteFilterCommand::class)) {
                return@DashboardComboBox ActionResult()
                    .withRedraw()
            }

            inviteFilter.action = SPBlockData.ActionList.values()[it.data.toInt()]
            ActionResult()
        }
        action.selectedValues = actions.filter { it.id.toInt() == inviteFilter.action.ordinal }
        container.add(action)

        return container
    }

    private fun generateInviteFilterExcludedField(inviteFilter: SPBlockData): DashboardComponent {
        val container = HorizontalContainer()
        container.allowWrap = true

        val ignoredUsers = DashboardMultiMembersComboBox(
            getString(Category.MODERATION, "invitefilter_state0_mignoredusers"),
            atomicGuild.idLong,
            inviteFilter.ignoredUserIds,
            true,
            InviteFilterCommand.MAX_IGNORED_USERS,
            atomicMember.idLong,
            InviteFilterCommand::class
        )
        container.add(ignoredUsers)

        val ignoredChannels = DashboardMultiTextChannelsComboBox(
            getString(Category.MODERATION, "invitefilter_state0_mignoredchannels"),
            atomicGuild.idLong,
            inviteFilter.ignoredChannelIds,
            true,
            InviteFilterCommand.MAX_IGNORED_CHANNELS,
            atomicMember.idLong,
            InviteFilterCommand::class
        )
        container.add(ignoredChannels)

        return container;
    }

    private fun generateWordFilterField(): DashboardComponent {
        val wordFilter = DBBannedWords.getInstance().retrieve(atomicGuild.idLong)
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.MODERATION, "wordfilter_title")))

        val activeSwitch = DashboardSwitch(getString(Category.MODERATION, "wordfilter_state0_menabled")) {
            if (!anyCommandsAreAccessible(WordFilterCommand::class)) {
                return@DashboardSwitch ActionResult()
                    .withRedraw()
            }

            wordFilter.isActive = it.data
            ActionResult()
                .withRedraw()
        }
        activeSwitch.isChecked = wordFilter.isActive
        container.add(activeSwitch, DashboardSeparator())

        val ignoredUsers = DashboardMultiMembersComboBox(
            getString(Category.MODERATION, "wordfilter_state0_mignoredusers"),
            atomicGuild.idLong,
            wordFilter.ignoredUserIds,
            true,
            WordFilterCommand.MAX_IGNORED_USERS,
            atomicMember.idLong,
            WordFilterCommand::class
        )
        container.add(ignoredUsers)

        val logReceivers = DashboardMultiMembersComboBox(
            getString(Category.MODERATION, "wordfilter_state0_mlogreciever"),
            atomicGuild.idLong,
            wordFilter.logReceiverUserIds,
            true,
            WordFilterCommand.MAX_LOG_RECEIVERS,
            atomicMember.idLong,
            WordFilterCommand::class
        )
        container.add(logReceivers, DashboardSeparator(), generateWordsComboBox(wordFilter))

        return container
    }

    private fun generateWordsComboBox(wordFilter: BannedWordsData): DashboardComponent {
        val words = wordFilter.words
        val label = getString(Category.MODERATION, "wordfilter_state0_mwords")
        val comboBox = DashboardComboBox(label, emptyList(), true, WordFilterCommand.MAX_WORDS) {
            if (!anyCommandsAreAccessible(WordFilterCommand::class)) {
                return@DashboardComboBox ActionResult()
                    .withRedraw()
            }

            if (it.type == "add") {
                WordFilter.translateString(it.data).split(" ")
                    .filter { it.length > 0 }
                    .map { it.substring(0, Math.min(WordFilterCommand.MAX_LETTERS, it.length)) }
                    .filter { !words.contains(it) }
                    .forEach { words += it }
            } else if (it.type == "remove") {
                words.remove(it.data)
            }
            ActionResult()
                .withRedraw()
        }
        comboBox.allowCustomValues = true
        comboBox.selectedValues = words.map { DiscordEntity(it, it) }
        return comboBox
    }

    private fun generateAutoModConfigTitle(): DashboardComponent {
        return DashboardTitle(getString(Category.MODERATION, "mod_state${autoModConfigSlot!!.states[autoModConfigStep]}_title"))
    }

    private fun generateAutoModConfigText(): DashboardComponent {
        var state = autoModConfigSlot!!.states[autoModConfigStep]
        if (autoModConfigStep == 1) {
            state = 4
        }
        return DashboardText(getString(Category.MODERATION, "mod_state${state}_description", false).split("\n")[0])
    }

    private fun generateAutoModConfigTextField(): DashboardComponent {
        when (autoModConfigStep) {
            0 -> {
                val textField = DashboardNumberField("", 1, Int.MAX_VALUE.toLong()) {
                    autoModConfigTempValue = it.data.toInt()
                    ActionResult()
                }
                textField.value = 1
                textField.editButton = false
                return textField
            }
            1 -> {
                val textField = DashboardNumberField("", 1, Int.MAX_VALUE.toLong()) {
                    autoModConfigTempDays = it.data.toInt()
                    ActionResult()
                }
                textField.value = 1
                textField.editButton = false
                return textField
            }
            2 -> {
                val textField = DashboardDurationField("") {
                    autoModConfigTempDuration = it.data.toInt()
                    ActionResult()
                }
                textField.value = 1
                textField.editButton = false
                return textField
            }
            else -> {
                throw RuntimeException()
            }
        }
    }

    private fun generateAutoModConfigButtons(): DashboardComponent {
        val container = HorizontalContainer()

        val continueButtonText = if (autoModConfigStep < autoModConfigSlot!!.states.size - 1) {
            getString(TextManager.GENERAL, "continue2")
        } else {
            getString(Category.MODERATION, "mod_complete")
        }
        val continueButton = DashboardButton(continueButtonText) {
            if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                return@DashboardButton ActionResult()
                    .withRedraw()
            }

            return@DashboardButton autoModConfigNextStep()
        }
        continueButton.style = DashboardButton.Style.PRIMARY
        container.add(continueButton)

        if (autoModConfigStep == 1) {
            val countAllButton = DashboardButton(getString(Category.MODERATION, "mod_state4_options")) {
                if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                    return@DashboardButton ActionResult()
                        .withRedraw()
                }

                autoModConfigTempDays = 0
                return@DashboardButton autoModConfigNextStep()
            }
            countAllButton.style = DashboardButton.Style.PRIMARY
            container.add(countAllButton)
        } else if (autoModConfigStep == 2) {
            val countAllButton = DashboardButton(getString(Category.MODERATION, "mod_state${autoModConfigSlot!!.states[autoModConfigStep]}_options")) {
                if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                    return@DashboardButton ActionResult()
                        .withRedraw()
                }

                autoModConfigTempDuration = 0
                return@DashboardButton autoModConfigNextStep()
            }
            countAllButton.style = DashboardButton.Style.PRIMARY
            container.add(countAllButton)
        }

        val cancelButton = DashboardButton(getString(TextManager.GENERAL, "process_abort")) {
            autoModConfigSlot = null
            ActionResult()
                .withRedrawScrollToTop()
        }
        cancelButton.style = DashboardButton.Style.DEFAULT
        container.add(cancelButton)

        container.add(HorizontalPusher())
        return container
    }

    private fun autoModConfigNextStep(): ActionResult {
        if (autoModConfigStep < autoModConfigSlot!!.states.size - 1) {
            autoModConfigStep++
            return ActionResult()
                .withRedrawScrollToTop()
        } else {
            val text = getString(Category.MODERATION, "mod_auto${autoModConfigSlot!!.id}set")
            val modData = DBModeration.getInstance().retrieve(atomicGuild.idLong)
            autoModConfigSlot!!.setData(modData, autoModConfigTempValue, autoModConfigTempDays, autoModConfigTempDuration)
            autoModConfigSlot = null
            return ActionResult()
                .withRedrawScrollToTop()
                .withSuccessMessage(text)
        }
    }

    enum class AutoModSlots(val id: String, vararg val states: Int?) {

        MUTE("mute", 8, 9, 10) {
            override fun getValue(modData: ModerationData): Int = modData.autoMute
            override fun getDays(modData: ModerationData): Int = modData.autoMuteDays
            override fun getDuration(modData: ModerationData): Int = modData.autoMuteDuration
            override fun setData(modData: ModerationData, value: Int, days: Int, duration: Int) = modData.setAutoMute(value, days, duration)
        },

        JAIL("jail", 13, 14, 15) {
            override fun getValue(modData: ModerationData): Int = modData.autoJail
            override fun getDays(modData: ModerationData): Int = modData.autoJailDays
            override fun getDuration(modData: ModerationData): Int = modData.autoJailDuration
            override fun setData(modData: ModerationData, value: Int, days: Int, duration: Int) = modData.setAutoJail(value, days, duration)
        },

        KICK("kick", 2, 4) {
            override fun getValue(modData: ModerationData): Int = modData.autoKick
            override fun getDays(modData: ModerationData): Int = modData.autoKickDays
            override fun getDuration(modData: ModerationData): Int = 0
            override fun setData(modData: ModerationData, value: Int, days: Int, duration: Int) = modData.setAutoKick(value, days)
        },

        BAN("ban", 3, 5, 7) {
            override fun getValue(modData: ModerationData): Int = modData.autoBan
            override fun getDays(modData: ModerationData): Int = modData.autoBanDays
            override fun getDuration(modData: ModerationData): Int = modData.autoBanDuration
            override fun setData(modData: ModerationData, value: Int, days: Int, duration: Int) = modData.setAutoBan(value, days, duration)
        };

        abstract fun getValue(modData: ModerationData): Int
        abstract fun getDays(modData: ModerationData): Int
        abstract fun getDuration(modData: ModerationData): Int
        abstract fun setData(modData: ModerationData, value: Int, days: Int, duration: Int)

    }

}