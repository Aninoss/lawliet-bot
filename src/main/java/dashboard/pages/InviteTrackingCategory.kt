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
import dashboard.components.DashboardMemberComboBox
import dashboard.components.DashboardTextChannelComboBox
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import dashboard.container.VerticalContainer
import dashboard.data.DiscordEntity
import dashboard.data.GridRow
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

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        val inviteTrackingData = DBInviteTracking.getInstance().retrieve(atomicGuild.idLong)

        if (anyCommandsAreAccessible(InviteTrackingCommand::class)) {
            mainContainer.add(
                generateActiveSwitch(inviteTrackingData),
                generateLogsField(inviteTrackingData)
            )
        }

        if (anyCommandsAreAccessible(InvitesManageCommand::class)) {
            mainContainer.add(
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

        val title = DashboardTitle(getString(Category.INVITE_TRACKING, "invitetracking_log_title"))
        container.add(title)

        val channelComboBox = DashboardTextChannelComboBox(
            getString(Category.INVITE_TRACKING, "invitetracking_state0_mchannel"),
            locale,
            atomicGuild.idLong,
            inviteTrackingData.textChannelId.orElse(null),
            true
        ) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardTextChannelComboBox ActionResult()
                    .withRedraw()
            }

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_LOG_CHANNEL, atomicMember, inviteTrackingData.textChannelId.orElse(null), it.data)
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
        container.add(DashboardTitle(getString(Category.INVITE_TRACKING, "invmanage_title")))

        if (inviteTrackingData.isActive) {
            container.add(generateInvitesManageMemberField(inviteTrackingData, premium))
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

    private fun generateInvitesManageMemberField(inviteTrackingData: InviteTrackingData, premium: Boolean): DashboardComponent {
        val container = VerticalContainer()
        val memberContainer = HorizontalContainer()
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

        if (manageMember != null) {
            val gridRows = inviteTrackingData.inviteTrackingSlots.values
                .filter { it.inviterUserId == manageMember }
                .map {
                    val atomicMember = AtomicMember(atomicGuild.idLong, it.memberId)
                    GridRow(it.memberId.toString(), arrayOf(atomicMember.getTaggedName(locale)))
                }

            if (!gridRows.isEmpty()) {
                val invitesGrid = DashboardGrid(arrayOf(getString(Category.INVITE_TRACKING, "invmanage_invitedmember")), gridRows) {
                    if (!anyCommandsAreAccessible(InvitesManageCommand::class)) {
                        return@DashboardGrid ActionResult()
                            .withRedraw()
                    }

                    entityManager.transaction.begin()
                    BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES, atomicMember, null, it.data, listOf(manageMember!!))
                    entityManager.transaction.commit()

                    FeatureLogger.inc(PremiumFeature.INVITE_TRACKING, atomicGuild.idLong)
                    inviteTrackingData.inviteTrackingSlots.remove(it.data.toLong())
                    ActionResult()
                        .withRedraw()
                }
                invitesGrid.isEnabled = premium
                invitesGrid.rowButton = getString(Category.INVITE_TRACKING, "invmanage_grid_delete")
                container.add(DashboardSeparator(), invitesGrid)

                val resetAllButton = DashboardButton(getString(Category.INVITE_TRACKING, "invmanage_resetall")) {
                    if (!anyCommandsAreAccessible(InvitesManageCommand::class)) {
                        return@DashboardButton ActionResult()
                            .withRedraw()
                    }

                    entityManager.transaction.begin()
                    BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES_RESET, atomicMember, null, null, listOf(manageMember!!))
                    entityManager.transaction.commit()

                    FeatureLogger.inc(PremiumFeature.INVITE_TRACKING, atomicGuild.idLong)
                    DBInviteTracking.getInstance().resetInviteTrackerSlotsOfInviter(atomicGuild.idLong, manageMember!!.toLong())
                    ActionResult()
                        .withRedraw()
                }
                resetAllButton.style = DashboardButton.Style.DANGER
                resetAllButton.isEnabled = premium
                resetAllButton.enableConfirmationMessage(getString(Category.INVITE_TRACKING, "invmanage_resetall_danger"))
                container.add(resetAllButton)
            }

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
            manageMemberComboBox.isEnabled = premium
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

                    FeatureLogger.inc(PremiumFeature.INVITE_TRACKING, atomicGuild.idLong)
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
            container.add(DashboardSeparator(), addNewContainer)

            if (addInviteMember != null && inviteTrackingData.inviteTrackingSlots.containsKey(addInviteMember)) {
                val text = DashboardText(getString(Category.INVITE_TRACKING, "invmanage_override"))
                text.style = DashboardText.Style.ERROR
                container.add(text)
            }
        }

        return container
    }

    fun clearAttributes() {
        manageMember = null
        addInviteMember = null
    }

}