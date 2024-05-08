package dashboard.pages

import commands.Category
import commands.Command
import commands.runnables.invitetrackingcategory.InviteTrackingCommand
import commands.runnables.invitetrackingcategory.InvitesManageCommand
import core.TextManager
import core.atomicassets.AtomicMember
import core.featurelogger.FeatureLogger
import core.featurelogger.PremiumFeature
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.*
import dashboard.components.DashboardChannelComboBox
import dashboard.components.DashboardMemberComboBox
import dashboard.container.DashboardListContainer
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import modules.invitetracking.InviteTracking
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import mysql.modules.invitetracking.DBInviteTracking
import mysql.modules.invitetracking.InviteTrackingData
import mysql.modules.invitetracking.InviteTrackingSlot
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.time.LocalDate
import java.util.*

@DashboardProperties(
        id = "invitetracking",
        userPermissions = [Permission.MANAGE_SERVER],
        botPermissions = [Permission.MANAGE_SERVER],
        commandAccessRequirements = [InviteTrackingCommand::class, InvitesManageCommand::class]
)
class InviteTrackingCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    var manageMember: Long? = null
    var addInviteMember: Long? = null

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(InviteTrackingCommand::class.java, locale).title
    }

    override fun retrievePageDescription(): String? {
        return getString(Category.INVITE_TRACKING, "invitetracking_state0_description")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val inviteTrackingData = DBInviteTracking.getInstance().retrieve(atomicGuild.idLong)

        if (anyCommandsAreAccessible(InviteTrackingCommand::class)) {
            val innerContainer = VerticalContainer(generateActiveSwitch(inviteTrackingData))
            innerContainer.isCard = true

            mainContainer.add(
                    innerContainer,
                    DashboardTitle(getString(Category.INVITE_TRACKING, "invitetracking_log_title")),
                    DashboardText(getString(Category.INVITE_TRACKING, "invitetracking_log_desc") + "\n\n" + getString(Category.INVITE_TRACKING, "invitetracking_dashboard_advancedstats_hint")),
                    generateLogsField(inviteTrackingData)
            )
        }

        if (anyCommandsAreAccessible(InvitesManageCommand::class)) {
            mainContainer.add(
                    DashboardTitle(getString(Category.INVITE_TRACKING, "invmanage_title")),
                    DashboardText(getString(Category.INVITE_TRACKING, "invmanage_description")),
                    generateInvitesManageField(inviteTrackingData)
            )
        }
    }

    fun generateActiveSwitch(inviteTrackingData: InviteTrackingData): DashboardComponent {
        val activeSwitch = DashboardSwitch(getString(Category.INVITE_TRACKING, "invitetracking_state0_mactive")) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            clearAttributes()
            inviteTrackingData.isActive = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_ACTIVE, atomicMember, null, it.data)
            entityManager.transaction.commit()

            if (inviteTrackingData.isActive) {
                InviteTracking.synchronizeGuildInvites(atomicGuild.get().orElseThrow(), locale)
            }
            ActionResult()
                    .withRedraw()
        }
        activeSwitch.isChecked = inviteTrackingData.isActive

        return activeSwitch
    }

    fun generateLogsField(inviteTrackingData: InviteTrackingData): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true
        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.INVITE_TRACKING, "invitetracking_state0_mchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                inviteTrackingData.channelId.orElse(null),
                true,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
        ) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardChannelComboBox ActionResult()
                        .withRedraw()
            }

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_LOG_CHANNEL, atomicMember, inviteTrackingData.channelId.orElse(null), it.data)
            entityManager.transaction.commit()

            inviteTrackingData.setChannelId(it.data?.toLong())
            ActionResult()
        }
        container.add(channelComboBox, DashboardSeparator())

        val pingMembersSwitch = DashboardSwitch(getString(Category.INVITE_TRACKING, "invitetracking_state0_mping")) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            inviteTrackingData.ping = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_PING_MEMBERS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        pingMembersSwitch.isChecked = inviteTrackingData.ping
        container.add(pingMembersSwitch, DashboardSeparator())

        val advancedSwitch = DashboardSwitch(getString(Category.INVITE_TRACKING, "invitetracking_state0_madvanced")) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            inviteTrackingData.isAdvanced = it.data

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_ADVANCED_STATISTICS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        advancedSwitch.isChecked = inviteTrackingData.isAdvanced
        container.add(advancedSwitch, DashboardSeparator())

        val resetButton = DashboardButton(getString(Category.INVITE_TRACKING, "invitetracking_dashboard_reset")) {
            DBInviteTracking.getInstance().resetInviteTrackerSlots(atomicGuild.idLong)

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_RESET, atomicMember)
            entityManager.transaction.commit()

            ActionResult()
                    .withRedraw()
        }
        resetButton.enableConfirmationMessage(getString(Category.INVITE_TRACKING, "invitetracking_dashboard_danger"))
        resetButton.style = DashboardButton.Style.DANGER
        container.add(HorizontalContainer(resetButton, HorizontalPusher()))

        return container
    }

    fun generateInvitesManageField(inviteTrackingData: InviteTrackingData): DashboardComponent {
        val premium = isPremium
        val container = VerticalContainer()

        if (inviteTrackingData.isActive) {
            container.add(generateInvitesManageMemberField(premium))

            if (manageMember != null) {
                val invitedSlots = inviteTrackingData.inviteTrackingSlots.values
                        .filter { it.inviterUserId == manageMember }

                val listAddContainer = VerticalContainer()
                listAddContainer.isCard = true
                if (invitedSlots.isNotEmpty()) {
                    listAddContainer.add(
                            generateInvitesManageListField(inviteTrackingData, invitedSlots, premium),
                            DashboardSeparator(true)
                    )
                }
                listAddContainer.add(generateInvitesManageAddField(inviteTrackingData, premium))
                container.add(listAddContainer)
            }
        } else {
            container.add(DashboardText(getString(Category.INVITE_TRACKING, "invmanage_notactive")))
        }

        if (!premium) {
            val text = DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }
        return container
    }

    private fun generateInvitesManageMemberField(premium: Boolean): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true

        val memberContainer = HorizontalContainer()
        memberContainer.allowWrap = true
        memberContainer.alignment = HorizontalContainer.Alignment.BOTTOM

        val manageMemberComboBox = DashboardMemberComboBox(
                getString(Category.INVITE_TRACKING, "invmanage_member"),
                locale,
                atomicGuild.idLong,
                manageMember,
                true
        ) {
            if (!anyCommandsAreAccessible(InvitesManageCommand::class)) {
                return@DashboardMemberComboBox ActionResult()
                        .withRedraw()
            }

            manageMember = it.data?.toLong()
            addInviteMember = null
            ActionResult()
                    .withRedraw()
        }
        manageMemberComboBox.isEnabled = premium
        if (manageMember != null && manageMember == 0L) {
            manageMemberComboBox.selectedValues = listOf(DiscordEntity("0", getString(TextManager.GENERAL, "invites_vanity")))
        }
        memberContainer.add(manageMemberComboBox)

        val vanityInviteButton = DashboardButton(getString(Category.INVITE_TRACKING, "invitetracking_dashboard_selectvanity")) {
            if (!anyCommandsAreAccessible(InvitesManageCommand::class)) {
                return@DashboardButton ActionResult()
                        .withRedraw()
            }

            manageMember = 0L
            addInviteMember = null
            ActionResult()
                    .withRedraw()
        }
        vanityInviteButton.isEnabled = premium
        vanityInviteButton.setCanExpand(false)
        memberContainer.add(vanityInviteButton)

        container.add(memberContainer)
        return container
    }

    private fun generateInvitesManageListField(inviteTrackingData: InviteTrackingData, invitedSlots: List<InviteTrackingSlot>, premium: Boolean): DashboardComponent {
        val container = VerticalContainer()
        val itemRows = invitedSlots
                .map { slot ->
                    val atomicMember = AtomicMember(atomicGuild.idLong, slot.memberId)

                    val itemContainer = HorizontalContainer()
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    itemContainer.add(DashboardText(atomicMember.getUsername(locale)), HorizontalPusher())

                    val deleteButton = DashboardButton(getString(Category.INVITE_TRACKING, "invmanage_grid_delete")) {
                        if (!anyCommandsAreAccessible(InvitesManageCommand::class)) {
                            return@DashboardButton ActionResult()
                                    .withRedraw()
                        }

                        entityManager.transaction.begin()
                        BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES, atomicMember, null, slot.memberId, listOf(manageMember!!))
                        entityManager.transaction.commit()

                        FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, atomicGuild.idLong)
                        inviteTrackingData.inviteTrackingSlots.remove(slot.memberId)
                        ActionResult()
                                .withRedraw()
                    }
                    deleteButton.isEnabled = premium
                    deleteButton.style = DashboardButton.Style.DANGER
                    itemContainer.add(deleteButton)
                    return@map itemContainer
                }

        val listCategory = DashboardListContainer()
        listCategory.add(itemRows)
        container.add(DashboardText(getString(Category.INVITE_TRACKING, "invitetracking_dashboard_invitedmembers")), listCategory)

        val resetAllButton = DashboardButton(getString(Category.INVITE_TRACKING, "invmanage_resetall")) {
            if (!anyCommandsAreAccessible(InvitesManageCommand::class)) {
                return@DashboardButton ActionResult()
                        .withRedraw()
            }

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES_RESET, atomicMember, null, null, listOf(manageMember!!))
            entityManager.transaction.commit()

            FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, atomicGuild.idLong)
            DBInviteTracking.getInstance().resetInviteTrackerSlotsOfInviter(atomicGuild.idLong, manageMember!!.toLong())
            ActionResult()
                    .withRedraw()
        }
        resetAllButton.style = DashboardButton.Style.DANGER
        resetAllButton.isEnabled = premium
        resetAllButton.enableConfirmationMessage(getString(Category.INVITE_TRACKING, "invmanage_resetall_danger"))
        container.add(resetAllButton)
        return container
    }

    private fun generateInvitesManageAddField(inviteTrackingData: InviteTrackingData, premium: Boolean): DashboardComponent {
        val container = VerticalContainer()
        val addNewContainer = HorizontalContainer()
        addNewContainer.allowWrap = true
        addNewContainer.alignment = HorizontalContainer.Alignment.BOTTOM

        val addInviteMemberComboBox = DashboardMemberComboBox(
                getString(Category.INVITE_TRACKING, "invmanage_invitedmember"),
                locale,
                atomicGuild.idLong,
                addInviteMember,
                true
        ) {
            addInviteMember = it.data?.toLong()
            ActionResult()
                    .withRedraw()
        }
        addInviteMemberComboBox.isEnabled = premium
        addNewContainer.add(addInviteMemberComboBox)

        val addInviteButton = DashboardButton(getString(Category.INVITE_TRACKING, "invmanage_state1_title")) {
            if (!anyCommandsAreAccessible(InvitesManageCommand::class)) {
                return@DashboardButton ActionResult()
                        .withRedraw()
            }

            if (addInviteMember != null) {
                val inviteTrackingSlot =
                        InviteTrackingSlot(atomicGuild.idLong, addInviteMember!!, manageMember!!, LocalDate.now(), LocalDate.now(), true)

                entityManager.transaction.begin()
                BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES, atomicMember, addInviteMember!!, null, listOf(manageMember!!))
                entityManager.transaction.commit()

                FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, atomicGuild.idLong)
                inviteTrackingData.inviteTrackingSlots[inviteTrackingSlot.memberId] = inviteTrackingSlot
                addInviteMember = null
                ActionResult()
                        .withRedraw()
            } else {
                ActionResult()
                        .withErrorMessage(getString(Category.INVITE_TRACKING, "invmanage_unknownmember"))
            }
        }
        addInviteButton.style = DashboardButton.Style.PRIMARY
        addInviteButton.isEnabled = premium
        addInviteButton.setCanExpand(false)
        addNewContainer.add(addInviteButton)
        container.add(addNewContainer)

        if (addInviteMember != null && inviteTrackingData.inviteTrackingSlots.containsKey(addInviteMember)) {
            val text = DashboardText(getString(Category.INVITE_TRACKING, "invmanage_override"))
            text.style = DashboardText.Style.ERROR
            container.add(text)
        }

        return container
    }

    fun clearAttributes() {
        manageMember = null
        addInviteMember = null
    }

}