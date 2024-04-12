package dashboard.pages

import commands.Category
import commands.runnables.moderationcategory.InviteFilterCommand
import commands.runnables.moderationcategory.ModSettingsCommand
import commands.runnables.moderationcategory.WordFilterCommand
import constants.ExternalLinks
import core.TextManager
import core.utils.BotPermissionUtil
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
import modules.automod.WordFilter
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.*

@DashboardProperties(
        id = "moderation",
        userPermissions = [Permission.MANAGE_SERVER, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS],
        botPermissions = [Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS, Permission.MODERATE_MEMBERS],
        commandAccessRequirements = [ModSettingsCommand::class, InviteFilterCommand::class, WordFilterCommand::class]
)
class ModerationCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var autoModConfigSlot: AutoModSlots? = null
    var autoModConfigStep = 0
    var autoModConfigTempValue: Int? = 1
    var autoModConfigTempDays: Int? = 1
    var autoModConfigTempDuration: Int? = 1

    val moderationEntity: ModerationEntity
        get() = guildEntity.moderation
    val inviteFilterEntity: InviteFilterEntity
        get() = guildEntity.inviteFilter
    val wordFilterEntity: WordFilterEntity
        get() = guildEntity.wordFilter

    override fun retrievePageTitle(): String {
        return getString(TextManager.COMMANDS, "moderation")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (autoModConfigSlot == null) {
            if (anyCommandsAreAccessible(ModSettingsCommand::class)) {
                mainContainer.add(
                        generateGeneralConfigurationField(),
                        generateBanAppealField(),
                        generateAutoModField()
                )
            }

            if (anyCommandsAreAccessible(InviteFilterCommand::class)) {
                mainContainer.add(generateInviteFilterField())
            }

            if (anyCommandsAreAccessible(WordFilterCommand::class)) {
                mainContainer.add(generateWordFilterField())
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

    fun generateBanAppealField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(
                DashboardTitle(getString(Category.MODERATION, "mod_banappeals")),
                DashboardText(getString(Category.MODERATION, "mod_banappeals_desc", ExternalLinks.BAN_APPEAL_URL + atomicGuild.idLong)),
                generateBanAppealLogChannelComponent()
        )
        return container
    }

    fun generateNotificationChannelComponent(): DashboardComponent {
        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.MODERATION, "mod_state0_mchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                moderationEntity.logChannelId,
                true
        ) { e ->
            if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                return@DashboardChannelComboBox ActionResult()
                        .withRedraw()
            }

            if (e.data != null) {
                val channel = atomicGuild.get()
                        .map { it.getChannelById(GuildMessageChannel::class.java, e.data) }
                        .orElse(null)

                if (channel == null) {
                    return@DashboardChannelComboBox ActionResult()
                            .withRedraw()
                }

                if (!BotPermissionUtil.canWriteEmbed(channel)) {
                    return@DashboardChannelComboBox ActionResult()
                            .withRedraw()
                            .withErrorMessage(getString(TextManager.GENERAL, "permission_channel", "#${channel.getName()}"))
                }
            }

            moderationEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.MOD_NOTIFICATION_CHANNEL, atomicMember, moderationEntity.logChannelId, e.data)
            moderationEntity.logChannelId = e.data?.toLong()
            moderationEntity.commitTransaction()
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

            moderationEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.MOD_CONFIRMATION_MESSAGES, atomicMember, null, it.data)
            moderationEntity.confirmationMessages = it.data
            moderationEntity.commitTransaction()
            ActionResult()
        }
        switch.isChecked = moderationEntity.confirmationMessages
        return switch
    }

    fun generateJailRolesComponent(): DashboardComponent {
        return DashboardMultiRolesComboBox(
                this,
                getString(Category.MODERATION, "mod_state0_mjailroles"),
                { it.moderation.jailRoleIds },
                true,
                ModSettingsCommand.MAX_JAIL_ROLES,
                true,
                ModSettingsCommand::class,
                BotLogEntity.Event.MOD_JAIL_ROLES
        )
    }

    fun generateBanAppealLogChannelComponent(): DashboardComponent {
        val container = VerticalContainer()
        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.MODERATION, "mod_dashboard_banappeallogchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                moderationEntity.banAppealLogChannelIdEffectively,
                true
        ) { e ->
            if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                return@DashboardChannelComboBox ActionResult()
                        .withRedraw()
            }

            if (e.data != null) {
                val channel = atomicGuild.get()
                        .map { it.getChannelById(GuildMessageChannel::class.java, e.data) }
                        .orElse(null)

                if (channel == null) {
                    return@DashboardChannelComboBox ActionResult()
                            .withRedraw()
                }

                if (!BotPermissionUtil.canWriteEmbed(channel)) {
                    return@DashboardChannelComboBox ActionResult()
                            .withRedraw()
                            .withErrorMessage(getString(TextManager.GENERAL, "permission_channel", "#${channel.getName()}"))
                }
            }

            moderationEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.MOD_BAN_APPEAL_LOG_CHANNEL, atomicMember, moderationEntity.banAppealLogChannelIdEffectively, e.data)
            moderationEntity.banAppealLogChannelId = e.data?.toLong()
            moderationEntity.commitTransaction()
            ActionResult()
        }
        channelComboBox.isEnabled = isPremium
        container.add(channelComboBox)

        if (!isPremium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }

        return container;
    }

    fun generateAutoModField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(
                DashboardTitle(getString(Category.MODERATION, "mod_state0_mautomod")),
                generateAutoModSlotField(AutoModSlots.MUTE),
                DashboardSeparator(),
                generateAutoModSlotField(AutoModSlots.JAIL),
                DashboardSeparator(),
                generateAutoModSlotField(AutoModSlots.KICK),
                DashboardSeparator(),
                generateAutoModSlotField(AutoModSlots.BAN)
        )
        return container
    }

    fun generateAutoModSlotField(slot: AutoModSlots): DashboardComponent {
        val infractions = slot.getInfractions(moderationEntity)
        val infractionDays = slot.getInfractionDays(moderationEntity)
        val durationMinutes = slot.getDurationMinutes(moderationEntity)

        val container = HorizontalContainer()
        container.allowWrap = true
        container.alignment = HorizontalContainer.Alignment.CENTER

        val name = getString(Category.MODERATION, "mod_auto${slot.id}")
        val status = if (infractions == null) {
            getString(Category.MODERATION, "mod_off")
        } else {
            ModSettingsCommand.getAutoModString(locale, infractions, infractionDays, durationMinutes).replace("*", "")
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

        if (slot.getInfractions(moderationEntity) != null) {
            val turnOffButton = DashboardButton(getString(Category.MODERATION, "mod_turnoff")) {
                if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                    return@DashboardButton ActionResult()
                            .withRedraw()
                }

                moderationEntity.beginTransaction()
                slot.setData(moderationEntity, null, null, null)
                BotLogEntity.log(entityManager, slot.eventDisable, atomicMember)
                moderationEntity.commitTransaction()

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
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.MODERATION, "invitefilter_title")))

        val activeSwitch = DashboardSwitch(getString(Category.MODERATION, "invitefilter_state0_menabled")) {
            if (!anyCommandsAreAccessible(InviteFilterCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            inviteFilterEntity.beginTransaction()
            inviteFilterEntity.active = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_FILTER_ACTIVE, atomicMember, null, it.data)
            inviteFilterEntity.commitTransaction()

            ActionResult()
                    .withRedraw()
        }
        activeSwitch.isChecked = inviteFilterEntity.active
        container.add(activeSwitch, DashboardSeparator(), generateInviteFilterExcludedField())

        val logReceivers = DashboardMultiMembersComboBox(
                this,
                getString(Category.MODERATION, "invitefilter_state0_mlogreciever"),
                { it.inviteFilter.logReceiverUserIds },
                true,
                InviteFilterCommand.MAX_LOG_RECEIVERS,
                InviteFilterCommand::class,
                BotLogEntity.Event.INVITE_FILTER_LOG_RECEIVERS
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

            val newAction = InviteFilterEntity.Action.values()[it.data.toInt()]
            inviteFilterEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_FILTER_ACTION, atomicMember, inviteFilterEntity.action, newAction)
            inviteFilterEntity.action = newAction
            inviteFilterEntity.commitTransaction()

            ActionResult()
        }
        action.selectedValues = actions.filter { it.id.toInt() == inviteFilterEntity.action.ordinal }
        container.add(action)

        return container
    }

    private fun generateInviteFilterExcludedField(): DashboardComponent {
        val container = HorizontalContainer()
        container.allowWrap = true

        val ignoredUsers = DashboardMultiMembersComboBox(
                this,
                getString(Category.MODERATION, "invitefilter_state0_mignoredusers"),
                { it.inviteFilter.excludedMemberIds },
                true,
                InviteFilterCommand.MAX_EXCLUDED_MEMBERS,
                InviteFilterCommand::class,
                BotLogEntity.Event.INVITE_FILTER_EXCLUDED_MEMBERS
        )
        container.add(ignoredUsers)

        val ignoredChannels = DashboardMultiChannelsComboBox(
                this,
                getString(Category.MODERATION, "invitefilter_state0_mignoredchannels"),
                DashboardComboBox.DataType.GUILD_CHANNELS,
                { it.inviteFilter.excludedChannelIds },
                true,
                InviteFilterCommand.MAX_EXCLUDED_CHANNELS,
                InviteFilterCommand::class,
                BotLogEntity.Event.INVITE_FILTER_EXCLUDED_CHANNELS
        )
        container.add(ignoredChannels)

        return container;
    }

    private fun generateWordFilterField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(DashboardTitle(getString(Category.MODERATION, "wordfilter_title")))

        val activeSwitch = DashboardSwitch(getString(Category.MODERATION, "wordfilter_state0_menabled")) {
            if (!anyCommandsAreAccessible(WordFilterCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            wordFilterEntity.beginTransaction()
            wordFilterEntity.active = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.WORD_FILTER_ACTIVE, atomicMember, null, it.data)
            wordFilterEntity.commitTransaction()

            ActionResult()
                    .withRedraw()
        }
        activeSwitch.isChecked = wordFilterEntity.active
        container.add(activeSwitch, DashboardSeparator())

        val ignoredUsers = DashboardMultiMembersComboBox(
                this,
                getString(Category.MODERATION, "wordfilter_state0_mignoredusers"),
                { it.wordFilter.excludedMemberIds },
                true,
                WordFilterCommand.MAX_IGNORED_USERS,
                WordFilterCommand::class,
                BotLogEntity.Event.WORD_FILTER_EXCLUDED_MEMBERS
        )
        container.add(ignoredUsers)

        val logReceivers = DashboardMultiMembersComboBox(
                this,
                getString(Category.MODERATION, "wordfilter_state0_mlogreciever"),
                { it.wordFilter.logReceiverUserIds },
                true,
                WordFilterCommand.MAX_LOG_RECEIVERS,
                WordFilterCommand::class,
                BotLogEntity.Event.WORD_FILTER_LOG_RECEIVERS
        )
        container.add(logReceivers, DashboardSeparator(), generateWordsComboBox())

        return container
    }

    private fun generateWordsComboBox(): DashboardComponent {
        val label = getString(Category.MODERATION, "wordfilter_state0_mwords")
        val comboBox = DashboardComboBox(label, emptyList(), true, WordFilterCommand.MAX_WORDS) {
            if (!anyCommandsAreAccessible(WordFilterCommand::class)) {
                return@DashboardComboBox ActionResult()
                        .withRedraw()
            }

            if (it.type == "add") {
                val newWordsList = WordFilter.translateString(it.data).split(" ")
                        .filter { it.length > 0 }
                        .map { it.substring(0, Math.min(WordFilterCommand.MAX_LETTERS, it.length)) }
                        .filter { !wordFilterEntity.words.contains(it) }

                wordFilterEntity.beginTransaction()
                newWordsList.forEach {
                    if (wordFilterEntity.words.size < WordFilterCommand.MAX_WORDS) {
                        wordFilterEntity.words += it
                    }
                }
                BotLogEntity.log(entityManager, BotLogEntity.Event.WORD_FILTER_WORDS, atomicMember, newWordsList, null)
                wordFilterEntity.commitTransaction()
            } else if (it.type == "remove") {
                wordFilterEntity.beginTransaction()
                wordFilterEntity.words -= it.data
                BotLogEntity.log(entityManager, BotLogEntity.Event.WORD_FILTER_WORDS, atomicMember, null, it.data)
                wordFilterEntity.commitTransaction()
            }

            ActionResult()
                    .withRedraw()
        }
        comboBox.allowCustomValues = true
        comboBox.selectedValues = wordFilterEntity.words.map { DiscordEntity(it, it) }
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
                textField.value = autoModConfigSlot?.getInfractions(moderationEntity)?.toLong() ?: 1
                textField.editButton = false
                return textField
            }

            1 -> {
                val textField = DashboardNumberField("", 1, Int.MAX_VALUE.toLong()) {
                    autoModConfigTempDays = it.data.toInt()
                    ActionResult()
                }
                textField.value = autoModConfigSlot?.getInfractionDays(moderationEntity)?.toLong() ?: 1
                textField.editButton = false
                return textField
            }

            2 -> {
                val textField = DashboardDurationField("") {
                    autoModConfigTempDuration = it.data.toInt()
                    ActionResult()
                }
                textField.value = autoModConfigSlot?.getDurationMinutes(moderationEntity)?.toLong() ?: 1
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
            val countAllButton = DashboardButton(getString(Category.MODERATION, "mod_automod_countall")) {
                if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                    return@DashboardButton ActionResult()
                            .withRedraw()
                }

                autoModConfigTempDays = null
                return@DashboardButton autoModConfigNextStep()
            }
            countAllButton.style = DashboardButton.Style.PRIMARY
            container.add(countAllButton)
        } else if (autoModConfigStep == 2) {
            val permanentlyButton = DashboardButton(getString(Category.MODERATION, "mod_state${autoModConfigSlot!!.states[autoModConfigStep]}_options").split("\n")[1]) {
                if (!anyCommandsAreAccessible(ModSettingsCommand::class)) {
                    return@DashboardButton ActionResult()
                            .withRedraw()
                }

                autoModConfigTempDuration = null
                return@DashboardButton autoModConfigNextStep()
            }
            permanentlyButton.style = DashboardButton.Style.PRIMARY
            container.add(permanentlyButton)
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

            moderationEntity.beginTransaction()
            logAutoMod(autoModConfigSlot!!.eventWarns, autoModConfigSlot!!.getInfractions(moderationEntity), autoModConfigTempValue)
            logAutoMod(autoModConfigSlot!!.eventWarnDays, autoModConfigSlot!!.getInfractionDays(moderationEntity), autoModConfigTempDays)
            if (autoModConfigStep == 2) {
                logAutoMod(autoModConfigSlot!!.eventDuration!!, autoModConfigSlot!!.getDurationMinutes(moderationEntity), autoModConfigTempDuration)
            }
            autoModConfigSlot!!.setData(moderationEntity, autoModConfigTempValue, autoModConfigTempDays, autoModConfigTempDuration)
            moderationEntity.commitTransaction()

            autoModConfigSlot = null
            return ActionResult()
                    .withRedrawScrollToTop()
                    .withSuccessMessage(text)
        }
    }

    private fun logAutoMod(event: BotLogEntity.Event, value0: Int?, value1: Int?) {
        BotLogEntity.log(entityManager, event, atomicMember, value0, value1)
    }

    enum class AutoModSlots(val id: String,
                            val eventDisable: BotLogEntity.Event, val eventWarns: BotLogEntity.Event,
                            val eventWarnDays: BotLogEntity.Event, val eventDuration: BotLogEntity.Event?,
                            vararg val states: Int?
    ) {

        MUTE("mute", BotLogEntity.Event.MOD_AUTO_MUTE_DISABLE, BotLogEntity.Event.MOD_AUTO_MUTE_WARNS,
                BotLogEntity.Event.MOD_AUTO_MUTE_WARN_DAYS, BotLogEntity.Event.MOD_AUTO_MUTE_DURATION, 8, 9, 10
        ) {
            override fun getAutoModEntity(moderationEntity: ModerationEntity): AutoModEntity = moderationEntity.autoMute
        },

        JAIL("jail", BotLogEntity.Event.MOD_AUTO_JAIL_DISABLE, BotLogEntity.Event.MOD_AUTO_JAIL_WARNS,
                BotLogEntity.Event.MOD_AUTO_JAIL_WARN_DAYS, BotLogEntity.Event.MOD_AUTO_JAIL_DURATION, 13, 14, 15
        ) {
            override fun getAutoModEntity(moderationEntity: ModerationEntity): AutoModEntity = moderationEntity.autoJail
        },

        KICK("kick", BotLogEntity.Event.MOD_AUTO_KICK_DISABLE, BotLogEntity.Event.MOD_AUTO_KICK_WARNS,
                BotLogEntity.Event.MOD_AUTO_KICK_WARN_DAYS, null, 2, 4
        ) {
            override fun getAutoModEntity(moderationEntity: ModerationEntity): AutoModEntity = moderationEntity.autoKick
        },

        BAN("ban", BotLogEntity.Event.MOD_AUTO_BAN_DISABLE, BotLogEntity.Event.MOD_AUTO_BAN_WARNS,
                BotLogEntity.Event.MOD_AUTO_BAN_WARN_DAYS, BotLogEntity.Event.MOD_AUTO_BAN_DURATION, 3, 5, 7
        ) {
            override fun getAutoModEntity(moderationEntity: ModerationEntity): AutoModEntity = moderationEntity.autoBan
        };

        abstract fun getAutoModEntity(moderationEntity: ModerationEntity): AutoModEntity
        fun getInfractions(moderationEntity: ModerationEntity): Int? = getAutoModEntity(moderationEntity).infractions
        fun getInfractionDays(moderationEntity: ModerationEntity): Int? = getAutoModEntity(moderationEntity).infractionDays
        fun getDurationMinutes(moderationEntity: ModerationEntity): Int? = getAutoModEntity(moderationEntity).durationMinutes
        fun setData(moderationEntity: ModerationEntity, infractions: Int?, infractionDays: Int?, durationMinutes: Int?) {
            val autoModEntity = getAutoModEntity(moderationEntity)

            autoModEntity.infractions = infractions
            autoModEntity.infractionDays = infractionDays
            if (this != KICK) {
                autoModEntity.durationMinutes = durationMinutes
            }
        }

    }

}