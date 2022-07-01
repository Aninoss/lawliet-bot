package dashboard.pages

import commands.Category
import commands.runnables.moderationcategory.ModSettingsCommand
import core.TextManager
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardMultiRolesComboBox
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import mysql.modules.moderation.DBModeration
import mysql.modules.moderation.ModerationData
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
    id = "moderation"
)
class ModerationCategory(guildId: Long, userId: Long, locale: Locale) : DashboardCategory(guildId, userId, locale) {

    var currentAutoModConfig: AutoModSlots? = null

    override fun retrievePageTitle(): String {
        return getString(TextManager.COMMANDS, "moderation")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (currentAutoModConfig == null) {
            mainContainer.add(
                generateGeneralConfigurationField(),
                generateAutoModField()
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
            DBModeration.getInstance().retrieve(atomicGuild.idLong).setAnnouncementChannelId(it.data?.toLong())
        }
        return channelComboBox;
    }

    fun generateConfirmationMessageComponent(): DashboardComponent {
        val switch = DashboardSwitch(getString(Category.MODERATION, "mod_state0_mquestion")) {
            DBModeration.getInstance().retrieve(atomicGuild.idLong).question = it.data
            ActionResult(false)
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
            true
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

        val button = DashboardButton(getString(Category.MODERATION, "mod_config")) {
            this.currentAutoModConfig = slot
            ActionResult(true)
        }
        button.style = DashboardButton.Style.PRIMARY
        container.add(button)

        return container
    }

    enum class AutoModSlots(val id: String) {

        MUTE("mute") {
            override fun getValue(modData: ModerationData): Int = modData.autoMute
            override fun getDays(modData: ModerationData): Int = modData.autoMuteDays
            override fun getDuration(modData: ModerationData): Int = modData.autoMuteDuration
        },

        JAIL("jail") {
            override fun getValue(modData: ModerationData): Int = modData.autoJail
            override fun getDays(modData: ModerationData): Int = modData.autoJailDays
            override fun getDuration(modData: ModerationData): Int = modData.autoJailDuration
        },

        KICK("kick") {
            override fun getValue(modData: ModerationData): Int = modData.autoKick
            override fun getDays(modData: ModerationData): Int = modData.autoKickDays
            override fun getDuration(modData: ModerationData): Int = 0
        },

        BAN("ban") {
            override fun getValue(modData: ModerationData): Int = modData.autoBan
            override fun getDays(modData: ModerationData): Int = modData.autoBanDays
            override fun getDuration(modData: ModerationData): Int = modData.autoBanDuration
        };

        abstract fun getValue(modData: ModerationData): Int
        abstract fun getDays(modData: ModerationData): Int
        abstract fun getDuration(modData: ModerationData): Int

    }

}