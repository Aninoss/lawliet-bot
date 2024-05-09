package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.configurationcategory.MemberCountDisplayCommand
import core.TextManager
import core.atomicassets.AtomicVoiceChannel
import core.utils.BotPermissionUtil
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.container.DashboardListContainer
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
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
    var nameMask: String = ""

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(MemberCountDisplayCommand::class.java, locale).title
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (nameMask.isEmpty()) {
            clearAttributes()
        }

        mainContainer.add(DashboardText(getString(Category.CONFIGURATION, "mcdisplays_state0_description")))
        if (DBMemberCountDisplays.getInstance().retrieve(atomicGuild.idLong).memberCountDisplaySlots.isNotEmpty()) {
            mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, "mcdisplays_dashboard_active")),
                generateListContainer()
            )
        }

        mainContainer.add(
                DashboardTitle(getString(Category.CONFIGURATION, "mcdisplays_state1_title")),
                generateNewDisplayField()
        )
    }

    fun generateListContainer(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val items = DBMemberCountDisplays.getInstance().retrieve(atomicGuild.idLong).memberCountDisplaySlots.values
                .map { slot ->
                    val button = DashboardButton(getString(Category.CONFIGURATION, "mcdisplays_dashboard_gridremove")) {
                        val removedSlot = DBMemberCountDisplays.getInstance().retrieve(atomicGuild.idLong).memberCountDisplaySlots.remove(slot.voiceChannelId)
                        if (removedSlot != null) {
                            entityManager.transaction.begin()
                            BotLogEntity.log(entityManager, BotLogEntity.Event.MEMBER_COUNT_DISPLAYS_DISCONNECT, atomicMember, removedSlot.voiceChannelId)
                            entityManager.transaction.commit()
                        }

                        return@DashboardButton ActionResult()
                                .withRedraw()
                    }
                    button.style = DashboardButton.Style.DANGER

                    val itemContainer = HorizontalContainer(DashboardText(slot.mask), HorizontalPusher(), button)
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    return@map itemContainer
                }

        val listContainer = DashboardListContainer()
        listContainer.add(items)
        return listContainer
    }

    fun generateNewDisplayField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true
        container.add(generateNewDisplayPropertiesField())

        val buttonField = HorizontalContainer()
        val addButton = DashboardButton(getString(Category.CONFIGURATION, "mcdisplays_dashboard_add")) {
            val err = MemberCountDisplay.initialize(locale, atomicVoiceChannel, nameMask)
            if (err != null) {
                return@DashboardButton ActionResult()
                        .withErrorMessage(err)
            }

            DBMemberCountDisplays.getInstance().retrieve(atomicGuild.idLong).memberCountDisplaySlots.put(
                    atomicVoiceChannel!!.idLong,
                    MemberCountDisplaySlot(atomicGuild.idLong, atomicVoiceChannel!!.idLong, nameMask!!)
            )
            MemberCountDisplay.manage(locale, atomicGuild.get().get())

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.MEMBER_COUNT_DISPLAYS_ADD, atomicMember, atomicVoiceChannel!!.idLong)
            entityManager.transaction.commit()

            clearAttributes()
            ActionResult()
                    .withRedraw()
        }
        addButton.style = DashboardButton.Style.PRIMARY
        buttonField.add(addButton, HorizontalPusher())
        container.add(DashboardSeparator(true), buttonField)
        return container
    }

    fun generateNewDisplayPropertiesField(): DashboardComponent {
        val container = VerticalContainer()

        val channelLabel = getString(Category.CONFIGURATION, "mcdisplays_dashboard_vc")
        val channelComboBox = DashboardComboBox(channelLabel, DashboardComboBox.DataType.VOICE_CHANNELS, false, 1) {
            val newAtomicVoiceChannel = AtomicVoiceChannel(atomicGuild.idLong, it.data.toLong())

            val channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(locale, newAtomicVoiceChannel.get().get(),
                    Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS
            )
            if (channelMissingPerms != null) {
                return@DashboardComboBox ActionResult()
                        .withRedraw()
                        .withErrorMessage(channelMissingPerms)
            }

            atomicVoiceChannel = newAtomicVoiceChannel
            ActionResult()
        }
        if (atomicVoiceChannel != null) {
            channelComboBox.selectedValues = listOf(DiscordEntity(atomicVoiceChannel!!.idLong.toString(), atomicVoiceChannel!!.getPrefixedName(locale)))
        }
        container.add(channelComboBox, DashboardSeparator(true))

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
        nameMaskTextField.editButton = false
        nameMaskTextField.value = nameMask
        container.add(
                nameMaskTextField,
                DashboardText(getString(Category.CONFIGURATION, "mcdisplays_vars").replace("- ", ""), DashboardText.Style.HINT)
        )

        return container;
    }

    fun clearAttributes() {
        atomicVoiceChannel = null
        nameMask = TextManager.getString(guildEntity.locale, Category.CONFIGURATION, "mcdisplays_state1_example")
    }

}