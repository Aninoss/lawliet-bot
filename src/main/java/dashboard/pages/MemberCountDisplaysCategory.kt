package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.MemberCountDisplayCommand
import core.atomicassets.AtomicVoiceChannel
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import dashboard.data.GridRow
import modules.MemberCountDisplay
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.membercountdisplays.DBMemberCountDisplays
import mysql.modules.membercountdisplays.MemberCountDisplaySlot
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.*

@DashboardProperties(
        id = "mcdisplays",
        userPermissions = [Permission.MANAGE_SERVER],
        botPermissions = [Permission.VOICE_CONNECT],
        commandAccessRequirements = [MemberCountDisplayCommand::class]
)
class MemberCountDisplaysCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var atomicVoiceChannel: AtomicVoiceChannel? = null
    var nameMask: String? = null

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(MemberCountDisplayCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.add(
                generateGrid(),
                generateNewDisplayField()
        )
    }

    fun generateGrid(): DashboardComponent {
        val rows = DBMemberCountDisplays.getInstance().retrieve(atomicGuild.idLong).memberCountBeanSlots.values
                .map {
                    val atomicVoiceChannel = AtomicVoiceChannel(atomicGuild.idLong, it.voiceChannelId)
                    val values = arrayOf(atomicVoiceChannel.getPrefixedName(locale), it.mask)
                    GridRow(it.voiceChannelId.toString(), values)
                }

        val headers = getString(Category.CONFIGURATION, "mcdisplays_dashboard_gridheader").split('\n').toTypedArray()
        val grid = DashboardGrid(headers, rows) {
            val slot = DBMemberCountDisplays.getInstance().retrieve(atomicGuild.idLong).memberCountBeanSlots.remove(it.data.toLong())

            if (slot != null) {
                entityManager.transaction.begin()
                BotLogEntity.log(entityManager, BotLogEntity.Event.MEMBER_COUNT_DISPLAYS_DISCONNECT, atomicMember, slot.voiceChannelId)
                entityManager.transaction.commit()
            }

            ActionResult()
                    .withRedraw()
        }
        grid.rowButton = getString(Category.CONFIGURATION, "mcdisplays_dashboard_gridremove")
        return grid
    }

    fun generateNewDisplayField(): DashboardComponent {
        val container = VerticalContainer()
        container.add(
                DashboardTitle(getString(Category.CONFIGURATION, "mcdisplays_state1_title")),
                generateNewDisplayPropertiesField()
        )

        val buttonField = HorizontalContainer()
        val addButton = DashboardButton(getString(Category.CONFIGURATION, "mcdisplays_dashboard_add")) {
            val voiceChannel = atomicVoiceChannel?.get()?.orElse(null)
            if (voiceChannel == null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "mcdisplays_dashboard_invalidvc"))
            }
            if (nameMask == null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(getString(Category.CONFIGURATION, "mcdisplays_dashboard_invalidmask"))
            }

            val err = MemberCountDisplay.initialize(locale, voiceChannel)
            if (err != null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(err)
            }

            DBMemberCountDisplays.getInstance().retrieve(atomicGuild.idLong).memberCountBeanSlots.put(
                    voiceChannel.idLong,
                    MemberCountDisplaySlot(atomicGuild.idLong, voiceChannel.idLong, nameMask!!)
            )
            MemberCountDisplay.manage(locale, atomicGuild.get().get())

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.MEMBER_COUNT_DISPLAYS_ADD, atomicMember, voiceChannel.idLong)
            entityManager.transaction.commit()

            clearAttributes()
            ActionResult()
                    .withRedraw()
        }
        addButton.style = DashboardButton.Style.PRIMARY
        buttonField.add(addButton, HorizontalPusher())
        container.add(DashboardSeparator(), buttonField)
        return container
    }

    fun generateNewDisplayPropertiesField(): DashboardComponent {
        val container = VerticalContainer()

        val channelLabel = getString(Category.CONFIGURATION, "mcdisplays_dashboard_vc")
        val channelComboBox = DashboardComboBox(channelLabel, DashboardComboBox.DataType.VOICE_CHANNELS, false, 1) {
            val newAtomicVoiceChannel = AtomicVoiceChannel(atomicGuild.idLong, it.data.toLong())
            val err = MemberCountDisplay.checkChannel(locale, newAtomicVoiceChannel.get().get())
            if (err != null) {
                return@DashboardComboBox ActionResult()
                        .withRedraw()
                        .withErrorMessage(err)
            }

            atomicVoiceChannel = newAtomicVoiceChannel
            ActionResult()
        }
        if (atomicVoiceChannel != null) {
            channelComboBox.selectedValues = listOf(DiscordEntity(atomicVoiceChannel!!.idLong.toString(), atomicVoiceChannel!!.getPrefixedName(locale)))
        }
        container.add(channelComboBox, DashboardSeparator())

        val nameMaskLabel = getString(Category.CONFIGURATION, "mcdisplays_dashboard_mask")
        val nameMaskTextField = DashboardTextField(nameMaskLabel, 1, MemberCountDisplayCommand.MAX_NAME_MASK_LENGTH) {
            if (MemberCountDisplay.replaceVariables(it.data, "", "", "", "").equals(it.data)) {
                return@DashboardTextField ActionResult()
                        .withRedraw()
                        .withErrorMessage(getString(Category.CONFIGURATION, "mcdisplays_dashboard_novars"))
            }

            nameMask = it.data
            ActionResult()
        }
        nameMaskTextField.value = nameMask ?: ""
        container.add(nameMaskTextField, DashboardText(getString(Category.CONFIGURATION, "mcdisplays_vars").replace("-", "•")))

        return container;
    }

    fun clearAttributes() {
        atomicVoiceChannel = null
        nameMask = null
    }

}