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
import mysql.hibernate.entity.InviteTrackingSlotEntity
import mysql.hibernate.entity.guild.GuildEntity
import mysql.hibernate.entity.guild.InviteTrackingEntity
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

    val inviteTrackingEntity: InviteTrackingEntity
        get() = guildEntity.inviteTracking

    override fun retrievePageTitle(): String {
        return Command.getCommandLanguage(InviteTrackingCommand::class.java, locale).title
    }

    override fun retrievePageDescription(): String {
        return getString(Category.INVITE_TRACKING, "invitetracking_state0_description")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        if (anyCommandsAreAccessible(InviteTrackingCommand::class)) {
            val innerContainer = VerticalContainer(generateActiveSwitch())
            innerContainer.isCard = true

            mainContainer.add(
                    innerContainer,
                    DashboardTitle(getString(Category.INVITE_TRACKING, "invitetracking_log_title")),
                    DashboardText(getString(Category.INVITE_TRACKING, "invitetracking_log_desc") + "\n\n" + getString(Category.INVITE_TRACKING, "invitetracking_dashboard_advancedstats_hint")),
                    generateLogsField()
            )
        }

        if (anyCommandsAreAccessible(InvitesManageCommand::class)) {
            mainContainer.add(
                    DashboardTitle(getString(Category.INVITE_TRACKING, "invmanage_title")),
                    DashboardText(getString(Category.INVITE_TRACKING, "invmanage_description")),
                    generateInvitesManageField()
            )
        }
    }

    fun generateActiveSwitch(): DashboardComponent {
        val activeSwitch = DashboardSwitch(getString(Category.INVITE_TRACKING, "invitetracking_state0_mactive")) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            clearAttributes()

            entityManager.transaction.begin()
            inviteTrackingEntity.active = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_ACTIVE, atomicMember, null, it.data)
            if (inviteTrackingEntity.active) {
                InviteTracking.synchronizeGuildInvites(guildEntity, atomicGuild.get().get())
            }
            entityManager.transaction.commit()

            ActionResult()
                .withRedraw()
        }
        activeSwitch.isChecked = inviteTrackingEntity.active

        return activeSwitch
    }

    fun generateLogsField(): DashboardComponent {
        val container = VerticalContainer()
        container.isCard = true
        val channelComboBox = DashboardChannelComboBox(
                this,
                getString(Category.INVITE_TRACKING, "invitetracking_state0_mchannel"),
                DashboardComboBox.DataType.GUILD_MESSAGE_CHANNELS,
                inviteTrackingEntity.logChannelId,
                true,
                arrayOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
        ) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardChannelComboBox ActionResult()
                        .withRedraw()
            }

            entityManager.transaction.begin()
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_LOG_CHANNEL, atomicMember, inviteTrackingEntity.logChannelId, it.data)
            inviteTrackingEntity.logChannelId = it.data?.toLong()
            entityManager.transaction.commit()

            ActionResult()
        }
        container.add(channelComboBox, DashboardSeparator())

        val pingMembersSwitch = DashboardSwitch(getString(Category.INVITE_TRACKING, "invitetracking_state0_mping")) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            entityManager.transaction.begin()
            inviteTrackingEntity.ping = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_PING_MEMBERS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        pingMembersSwitch.isChecked = inviteTrackingEntity.ping
        container.add(pingMembersSwitch, DashboardSeparator())

        val advancedSwitch = DashboardSwitch(getString(Category.INVITE_TRACKING, "invitetracking_state0_madvanced")) {
            if (!anyCommandsAreAccessible(InviteTrackingCommand::class)) {
                return@DashboardSwitch ActionResult()
                        .withRedraw()
            }

            entityManager.transaction.begin()
            inviteTrackingEntity.advanced = it.data
            BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_ADVANCED_STATISTICS, atomicMember, null, it.data)
            entityManager.transaction.commit()

            ActionResult()
        }
        advancedSwitch.isChecked = inviteTrackingEntity.advanced
        container.add(advancedSwitch, DashboardSeparator())

        val resetButton = DashboardButton(getString(Category.INVITE_TRACKING, "invitetracking_dashboard_reset")) {
            entityManager.transaction.begin()
            inviteTrackingEntity.slots.clear()
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

    fun generateInvitesManageField(): DashboardComponent {
        val premium = isPremium
        val container = VerticalContainer()

        if (inviteTrackingEntity.active) {
            container.add(generateInvitesManageMemberField(premium))

            if (manageMember != null) {
                val invitedSlots = inviteTrackingEntity.slots.entries
                        .filter { it.value.inviterUserId == manageMember }

                val listAddContainer = VerticalContainer()
                listAddContainer.isCard = true
                if (invitedSlots.isNotEmpty()) {
                    listAddContainer.add(
                            generateInvitesManageListField(invitedSlots, premium),
                            DashboardSeparator(true)
                    )
                }
                listAddContainer.add(generateInvitesManageAddField(premium))
                container.add(listAddContainer)
            }
        } else {
            val innerContainer = VerticalContainer(DashboardText(getString(Category.INVITE_TRACKING, "invmanage_notactive")))
            innerContainer.isCard = true
            container.add(innerContainer)
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
        if (!premium) {
            container.add(DashboardText(getString(TextManager.GENERAL, "patreon_description_noembed"), DashboardText.Style.ERROR))
        }

        return container
    }

    private fun generateInvitesManageListField(invitedSlots: List<MutableMap.MutableEntry<Long, InviteTrackingSlotEntity>>, premium: Boolean): DashboardComponent {
        val container = VerticalContainer()
        val itemRows = invitedSlots
                .map { entry ->
                    val atomicMember = AtomicMember(atomicGuild.idLong, entry.key)

                    val itemContainer = HorizontalContainer()
                    itemContainer.alignment = HorizontalContainer.Alignment.CENTER
                    itemContainer.add(DashboardText(atomicMember.getUsername(locale)), HorizontalPusher())

                    val deleteButton = DashboardButton(getString(Category.INVITE_TRACKING, "invmanage_grid_delete")) {
                        if (!anyCommandsAreAccessible(InvitesManageCommand::class)) {
                            return@DashboardButton ActionResult()
                                    .withRedraw()
                        }

                        entityManager.transaction.begin()
                        BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES, atomicMember, null, entry.key, listOf(manageMember!!))
                        inviteTrackingEntity.slots.remove(entry.key)
                        entityManager.transaction.commit()

                        FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, atomicGuild.idLong)
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
            inviteTrackingEntity.removeSlotsOfInviter(manageMember!!.toLong())
            entityManager.transaction.commit()

            FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, atomicGuild.idLong)
            ActionResult()
                    .withRedraw()
        }
        resetAllButton.style = DashboardButton.Style.DANGER
        resetAllButton.isEnabled = premium
        resetAllButton.enableConfirmationMessage(getString(Category.INVITE_TRACKING, "invmanage_resetall_danger"))
        container.add(resetAllButton)
        return container
    }

    private fun generateInvitesManageAddField(premium: Boolean): DashboardComponent {
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
                        InviteTrackingSlotEntity(manageMember!!, LocalDate.now(), LocalDate.now(), true)

                entityManager.transaction.begin()
                BotLogEntity.log(entityManager, BotLogEntity.Event.INVITE_TRACKING_FAKE_INVITES, atomicMember, addInviteMember!!, null, listOf(manageMember!!))
                inviteTrackingEntity.slots[addInviteMember!!] = inviteTrackingSlot
                entityManager.transaction.commit()

                FeatureLogger.inc(PremiumFeature.INVITE_TRACKING_MANAGE, atomicGuild.idLong)
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

        if (addInviteMember != null && inviteTrackingEntity.slots.containsKey(addInviteMember)) {
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