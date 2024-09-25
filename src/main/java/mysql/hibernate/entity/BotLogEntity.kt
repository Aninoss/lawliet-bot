package mysql.hibernate.entity

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import commands.Category
import commands.Command
import commands.runnables.birthdaycategory.BirthdayConfigCommand
import commands.runnables.configurationcategory.*
import commands.runnables.fisherysettingscategory.FisheryCommand
import commands.runnables.fisherysettingscategory.FisheryManageCommand
import commands.runnables.fisherysettingscategory.FisheryRolesCommand
import commands.runnables.fisherysettingscategory.VCTimeCommand
import commands.runnables.invitetrackingcategory.InviteTrackingCommand
import commands.runnables.moderationcategory.InviteFilterCommand
import commands.runnables.moderationcategory.ModSettingsCommand
import commands.runnables.moderationcategory.WordFilterCommand
import core.MainLogger
import core.ShardManager
import core.atomicassets.AtomicMember
import core.utils.CollectionUtil
import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.template.HibernateEntity
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import javax.persistence.*
import kotlin.math.min


@Entity(name = "BotLog")
class BotLogEntity(
        @Id var id: UUID? = null,
        var guildId: Long = 0L,
        @Enumerated(EnumType.STRING) var event: Event = Event.NULL,
        var memberId: Long = 0L,
        @ElementCollection var targetMemberIds: List<Long> = emptyList(),
        var timestampCreate: Long = 0L,
        var timestampUpdate: Long? = null,
        @ElementCollection var values0: MutableList<String> = mutableListOf(),
        @ElementCollection var values1: MutableList<String> = mutableListOf(),
) : HibernateEntity() {

    enum class ValueType {
        INTEGER, LONG, DOUBLE, BOOLEAN, STRING,
        TEXT_KEY, COMMAND_CATEGORY, DURATION,
        CHANNEL, ROLE, USER
    }

    enum class ValuesRelationship {
        EMPTY, OLD_AND_NEW, ADD_AND_REMOVE, SINGLE_VALUE_COLUMN
    }

    enum class Event(val valuesRelationship: ValuesRelationship = ValuesRelationship.EMPTY, val valueType: ValueType? = null,
                     val commandClass: Class<out Command>? = null, val valueNameTextKey: String? = null, val valueNameTextCategory: Category? = null
    ) {
        NULL,
        LANGUAGE(ValuesRelationship.OLD_AND_NEW, ValueType.TEXT_KEY, LanguageCommand::class.java),
        PREFIX(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, PrefixCommand::class.java),
        AUTO_QUOTE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, AutoQuoteCommand::class.java),
        REMOVE_AUTHOR_MESSAGE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, TriggerDeleteCommand::class.java),
        COMMAND_MANAGEMENT(ValuesRelationship.ADD_AND_REMOVE, ValueType.COMMAND_CATEGORY, CommandManagementCommand::class.java),
        CHANNEL_WHITELIST(ValuesRelationship.ADD_AND_REMOVE, ValueType.CHANNEL, WhiteListCommand::class.java),
        COMMAND_PERMISSIONS_TRANSFER,
        FISHERY_STATUS(ValuesRelationship.OLD_AND_NEW, ValueType.TEXT_KEY, FisheryCommand::class.java, "fishery_state0_mstatus"),
        FISHERY_DATA_RESET,
        FISHERY_TREASURE_CHESTS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, FisheryCommand::class.java, "fishery_dashboard_treasurechests"),
        FISHERY_POWER_UPS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, FisheryCommand::class.java, "fishery_dashboard_powerups"),
        FISHERY_FISH_REMINDERS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, FisheryCommand::class.java, "fishery_dashboard_reminders"),
        FISHERY_COIN_GIFT_LIMIT(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, FisheryCommand::class.java, "fishery_dashboard_coingiftlimit"),
        FISHERY_TREASURE_CHEST_PROBABILITY(ValuesRelationship.OLD_AND_NEW, ValueType.DOUBLE, FisheryCommand::class.java, "fishery_probabilities_treasure"),
        FISHERY_POWER_UP_PROBABILITY(ValuesRelationship.OLD_AND_NEW, ValueType.DOUBLE, FisheryCommand::class.java, "fishery_probabilities_powerups"),
        FISHERY_WORK_INTERVAL(ValuesRelationship.OLD_AND_NEW, ValueType.DURATION, FisheryCommand::class.java, "fishery_dashboard_workinterval"),
        FISHERY_EXCLUDED_CHANNELS(ValuesRelationship.ADD_AND_REMOVE, ValueType.CHANNEL, FisheryCommand::class.java, "fishery_state0_mchannels"),
        FISHERY_VOICE_HOURS_LIMIT(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER, VCTimeCommand::class.java),
        FISHERY_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE, FisheryRolesCommand::class.java, "fisheryroles_state0_mroles"),
        FISHERY_ROLES_UPGRADE_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, FisheryRolesCommand::class.java, "fisheryroles_state0_mannouncementchannel"),
        FISHERY_ROLES_PRICE_MIN(ValuesRelationship.OLD_AND_NEW, ValueType.LONG, FisheryRolesCommand::class.java, "fisheryroles_firstprice"),
        FISHERY_ROLES_PRICE_MAX(ValuesRelationship.OLD_AND_NEW, ValueType.LONG, FisheryRolesCommand::class.java, "fisheryroles_lastprice"),
        FISHERY_ROLES_SINGLE_ROLES(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, FisheryRolesCommand::class.java, "fisheryroles_state0_msinglerole_raw"),
        FISHERY_MANAGE_FISH(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "fisherymanage_type_fish"),
        FISHERY_MANAGE_COINS(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "fisherymanage_type_coins"),
        FISHERY_MANAGE_DAILY_STREAK(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "fisherymanage_type_dailystreak"),
        FISHERY_MANAGE_ROD(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "buy_product_0_0", Category.FISHERY),
        FISHERY_MANAGE_ROBOT(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "buy_product_1_0", Category.FISHERY),
        FISHERY_MANAGE_NET(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "buy_product_2_0", Category.FISHERY),
        FISHERY_MANAGE_METAL_DETECTOR(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "buy_product_3_0", Category.FISHERY),
        FISHERY_MANAGE_ROLE(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "buy_product_4_0", Category.FISHERY),
        FISHERY_MANAGE_SURVEYS(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "buy_product_5_0", Category.FISHERY),
        FISHERY_MANAGE_WORK(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, FisheryManageCommand::class.java, "buy_product_6_0", Category.FISHERY),
        FISHERY_MANAGE_RESET,
        FISHERY_SPAWN_TREASURE_CHEST(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        FISHERY_SPAWN_POWERUP(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        ALERTS(ValuesRelationship.ADD_AND_REMOVE, ValueType.STRING, AlertsCommand::class.java),
        NSFW_FILTER(ValuesRelationship.ADD_AND_REMOVE, ValueType.STRING, NSFWFilterCommand::class.java),
        MOD_NOTIFICATION_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, ModSettingsCommand::class.java, "mod_state0_mchannel"),
        MOD_CONFIRMATION_MESSAGES(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, ModSettingsCommand::class.java, "mod_state0_mquestion"),
        MOD_JAIL_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE, ModSettingsCommand::class.java, "mod_state0_mjailroles"),
        MOD_BAN_APPEAL_LOG_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, ModSettingsCommand::class.java, "mod_state0_mbanappeallogchannel"),
        MOD_AUTO_MUTE_DISABLE,
        MOD_AUTO_MUTE_WARNS(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        MOD_AUTO_MUTE_WARN_DAYS(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        MOD_AUTO_MUTE_DURATION(ValuesRelationship.OLD_AND_NEW, ValueType.DURATION),
        MOD_AUTO_JAIL_DISABLE,
        MOD_AUTO_JAIL_WARNS(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        MOD_AUTO_JAIL_WARN_DAYS(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        MOD_AUTO_JAIL_DURATION(ValuesRelationship.OLD_AND_NEW, ValueType.DURATION),
        MOD_AUTO_KICK_DISABLE,
        MOD_AUTO_KICK_WARNS(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        MOD_AUTO_KICK_WARN_DAYS(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        MOD_AUTO_BAN_DISABLE,
        MOD_AUTO_BAN_WARNS(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        MOD_AUTO_BAN_WARN_DAYS(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        MOD_AUTO_BAN_DURATION(ValuesRelationship.OLD_AND_NEW, ValueType.DURATION),
        MOD_WARN,
        MOD_KICK,
        MOD_BAN,
        MOD_UNBAN,
        MOD_WARNREMOVE,
        MOD_MUTE,
        MOD_UNMUTE,
        MOD_JAIL,
        MOD_UNJAIL,
        MOD_CLEAR(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        MOD_FULLCLEAR(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        BAN_APPEAL_UNBAN,
        BAN_APPEAL_DECLINE,
        BAN_APPEAL_DECLINE_PERMANENTLY,
        INVITE_FILTER_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, InviteFilterCommand::class.java, "invitefilter_state0_menabled"),
        INVITE_FILTER_EXCLUDED_MEMBERS(ValuesRelationship.ADD_AND_REMOVE, ValueType.USER, InviteFilterCommand::class.java, "invitefilter_state0_mignoredusers"),
        INVITE_FILTER_EXCLUDED_CHANNELS(ValuesRelationship.ADD_AND_REMOVE, ValueType.CHANNEL, InviteFilterCommand::class.java, "invitefilter_state0_mignoredchannels"),
        INVITE_FILTER_LOG_RECEIVERS(ValuesRelationship.ADD_AND_REMOVE, ValueType.USER, InviteFilterCommand::class.java, "invitefilter_state0_mlogreciever"),
        INVITE_FILTER_ACTION(ValuesRelationship.OLD_AND_NEW, ValueType.TEXT_KEY, InviteFilterCommand::class.java, "invitefilter_state0_maction"),
        WORD_FILTER_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, WordFilterCommand::class.java, "wordfilter_state0_menabled"),
        WORD_FILTER_EXCLUDED_MEMBERS(ValuesRelationship.ADD_AND_REMOVE, ValueType.USER, WordFilterCommand::class.java, "wordfilter_state0_mignoredusers"),
        WORD_FILTER_LOG_RECEIVERS(ValuesRelationship.ADD_AND_REMOVE, ValueType.USER, WordFilterCommand::class.java, "wordfilter_state0_mlogreciever"),
        WORD_FILTER_WORDS(ValuesRelationship.ADD_AND_REMOVE, ValueType.STRING, WordFilterCommand::class.java, "wordfilter_state0_mwords"),
        INVITE_TRACKING_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, InviteTrackingCommand::class.java, "invitetracking_state0_mactive"),
        INVITE_TRACKING_LOG_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, InviteTrackingCommand::class.java, "invitetracking_state0_mchannel"),
        INVITE_TRACKING_PING_MEMBERS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, InviteTrackingCommand::class.java, "invitetracking_state0_mping"),
        INVITE_TRACKING_ADVANCED_STATISTICS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, InviteTrackingCommand::class.java, "invitetracking_state0_madvanced"),
        INVITE_TRACKING_FAKE_INVITES(ValuesRelationship.ADD_AND_REMOVE, ValueType.USER),
        INVITE_TRACKING_FAKE_INVITES_RESET,
        INVITE_TRACKING_RESET,
        GIVEAWAYS_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        GIVEAWAYS_EDIT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        GIVEAWAYS_END(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        GIVEAWAYS_REMOVE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        GIVEAWAYS_REROLL(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        REACTION_ROLES_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        REACTION_ROLES_EDIT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        AUTO_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE, AutoRolesCommand::class.java),
        AUTO_ROLES_SYNC,
        STICKY_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE, StickyRolesCommand::class.java),
        WELCOME_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, WelcomeCommand::class.java, "welcome_logs_welcome_active"),
        WELCOME_TEXT(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, WelcomeCommand::class.java, "welcome_logs_welcome_text"),
        WELCOME_EMBEDS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, WelcomeCommand::class.java, "welcome_logs_welcome_embed"),
        WELCOME_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, WelcomeCommand::class.java, "welcome_logs_welcome_channel"),
        WELCOME_BANNERS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, WelcomeCommand::class.java, "welcome_state0_mbanner"),
        WELCOME_ATTACHMENT_TYPE(ValuesRelationship.OLD_AND_NEW, ValueType.TEXT_KEY, WelcomeCommand::class.java, "welcome_state0_mattachmenttype"),
        WELCOME_BANNER_TITLE(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, WelcomeCommand::class.java, "welcome_state0_mtitle"),
        WELCOME_BANNER_BACKGROUND_SET,
        WELCOME_BANNER_BACKGROUND_RESET,
        WELCOME_IMAGE_SET,
        WELCOME_IMAGE_RESET,
        WELCOME_DM_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, WelcomeCommand::class.java, "welcome_logs_dm_active"),
        WELCOME_DM_TEXT(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, WelcomeCommand::class.java, "welcome_logs_dm_text"),
        WELCOME_DM_EMBEDS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, WelcomeCommand::class.java, "welcome_logs_dm_embed"),
        WELCOME_DM_IMAGE_SET,
        WELCOME_DM_IMAGE_RESET,
        WELCOME_LEAVE_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, WelcomeCommand::class.java, "welcome_logs_leave_active"),
        WELCOME_LEAVE_TEXT(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, WelcomeCommand::class.java, "welcome_logs_leave_text"),
        WELCOME_LEAVE_EMBEDS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, WelcomeCommand::class.java, "welcome_logs_leave_embed"),
        WELCOME_LEAVE_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, WelcomeCommand::class.java, "welcome_logs_leave_channel"),
        WELCOME_LEAVE_IMAGE_SET,
        WELCOME_LEAVE_IMAGE_RESET,
        TICKETS_CREATE_TICKET_MESSAGE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        TICKETS_LOG_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, TicketCommand::class.java, "ticket_state0_mannouncement"),
        TICKETS_STAFF_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE, TicketCommand::class.java, "ticket_state0_mstaffroles"),
        TICKETS_ASSIGNMENT_MODE(ValuesRelationship.OLD_AND_NEW, ValueType.TEXT_KEY, TicketCommand::class.java, "ticket_state0_massign"),
        TICKETS_PING_STAFF_ROLES(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, TicketCommand::class.java, "ticket_state0_mping"),
        TICKETS_ENFORCE_MODAL(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, TicketCommand::class.java, "ticket_state0_mtextinput"),
        TICKETS_GREETING_TEXT(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, TicketCommand::class.java, "ticket_state0_mcreatemessage"),
        TICKETS_MEMBERS_CAN_CLOSE_TICKETS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, TicketCommand::class.java, "ticket_state0_mmembercanclose"),
        TICKETS_PROTOCOLS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, TicketCommand::class.java, "ticket_state0_mprotocol"),
        TICKETS_DELETE_CHANNELS_ON_CLOSE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, TicketCommand::class.java, "ticket_state0_mdeletechannel"),
        TICKETS_AUTO_CLOSE(ValuesRelationship.OLD_AND_NEW, ValueType.DURATION, TicketCommand::class.java, "ticket_state0_mcloseoninactivity"),
        TICKETS_ASSIGN(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        TICKETS_CLOSE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        SERVER_SUGGESTIONS_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, SuggestionConfigCommand::class.java, "suggconfig_state0_mactive"),
        SERVER_SUGGESTIONS_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, SuggestionConfigCommand::class.java, "suggconfig_state0_mchannel"),
        SERVER_SUGGESTIONS_MANAGE_ACCEPT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.LONG),
        SERVER_SUGGESTIONS_MANAGE_DECLINE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.LONG),
        AUTO_CHANNEL_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, AutoChannelCommand::class.java, "autochannel_state0_mactive"),
        AUTO_CHANNEL_INITIAL_VOICE_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, AutoChannelCommand::class.java, "autochannel_state0_mchannel"),
        AUTO_CHANNEL_INITIAL_VOICE_CHANNELS(ValuesRelationship.ADD_AND_REMOVE, ValueType.CHANNEL, AutoChannelCommand::class.java, "autochannel_state0_mchannel"),
        AUTO_CHANNEL_NEW_CHANNEL_NAME(ValuesRelationship.OLD_AND_NEW, ValueType.STRING, AutoChannelCommand::class.java, "autochannel_state0_mchannelname"),
        AUTO_CHANNEL_BEGIN_LOCKED(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, AutoChannelCommand::class.java, "autochannel_state0_mlocked"),
        MEMBER_COUNT_DISPLAYS_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        MEMBER_COUNT_DISPLAYS_DISCONNECT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        CUSTOM_COMMANDS_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        CUSTOM_COMMANDS_EDIT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        CUSTOM_COMMANDS_DELETE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        COMMAND_CHANNEL_SHORTCUTS_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        COMMAND_CHANNEL_SHORTCUTS_DELETE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        REMINDERS_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        REMINDERS_EDIT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        REMINDERS_DELETE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        ASSIGN_ROLES(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.ROLE),
        REVOKE_ROLES(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.ROLE),
        SET_NSFW(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        SET_NOT_NSFW(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.CHANNEL),
        CUSTOM_ROLE_PLAY_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        CUSTOM_ROLE_PLAY_EDIT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        CUSTOM_ROLE_PLAY_DELETE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        BIRTHDAY_CONFIG_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN, BirthdayConfigCommand::class.java, "birthdayconfig_home_active"),
        BIRTHDAY_CONFIG_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL, BirthdayConfigCommand::class.java, "birthdayconfig_home_channel"),
        BIRTHDAY_CONFIG_ROLE(ValuesRelationship.OLD_AND_NEW, ValueType.ROLE, BirthdayConfigCommand::class.java, "birthdayconfig_home_role"),
    }


    val timeCreate: Instant
        get() = Instant.ofEpochSecond(timestampCreate)

    val timeUpdate: Instant?
        get() = if (timestampUpdate != null) {
            Instant.ofEpochSecond(timestampUpdate!!)
        } else {
            null
        }

    val targetedUserList: List<User>
        get() = targetMemberIds
                .map {
                    return@map try {
                        ShardManager.fetchUserById(it).get()
                    } catch (e: Throwable) {
                        // Ignore
                        null
                    }
                }
                .filter(Objects::nonNull)
                .map { it!! }


    constructor() : this(null)

    companion object {
        private val lastLogCache: Cache<Long, BotLogEntity> = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build()

        @JvmStatic
        @JvmOverloads
        fun log(entityManager: EntityManagerWrapper, event: Event, member: AtomicMember,
                values0: Any? = null, values1: Any? = null, targetMemberIds: List<Long>? = null
        ) {
            log(entityManager, event, member.guildId, member.idLong, values0, values1, targetMemberIds)
        }

        @JvmStatic
        @JvmOverloads
        fun log(entityManager: EntityManagerWrapper, event: Event, member: Member,
                values0: Any? = null, values1: Any? = null, targetMemberIds: List<Long>? = null
        ) {
            log(entityManager, event, member.guild.idLong, member.idLong, values0, values1, targetMemberIds)
        }

        @JvmStatic
        @JvmOverloads
        fun log(entityManager: EntityManagerWrapper, event: Event, guildId: Long, memberId: Long,
                values0: Any? = null, values1: Any? = null, targetMemberIds: List<Long>? = null
        ) {
            if (!entityManager.transaction.isActive) {
                MainLogger.get().error("Bot log failed due to no active transaction for event {}", event.name)
                return
            }

            val newValues0 = anyValueToMutableStringList(values0)
            val newValues1 = anyValueToMutableStringList(values1)
            val newTargetMemberIds: List<Long> = targetMemberIds?.subList(0, min(6, targetMemberIds.size))
                    ?: emptyList()

            if ((event.valuesRelationship == ValuesRelationship.OLD_AND_NEW && newValues0 == newValues1) ||
                    (event.valuesRelationship == ValuesRelationship.ADD_AND_REMOVE && newValues0.isEmpty() && newValues1.isEmpty())
            ) {
                return
            }

            val lastLog = lastLogCache.getIfPresent(guildId)
            if (lastLog != null && event == lastLog.event && memberId == lastLog.memberId && newTargetMemberIds == lastLog.targetMemberIds) {
                val logEntity = entityManager.find(BotLogEntity::class.java, lastLog.id)
                if (logEntity != null) {
                    logEntity.timestampUpdate = Instant.now().epochSecond

                    when (event.valuesRelationship) {
                        ValuesRelationship.EMPTY -> {}
                        ValuesRelationship.OLD_AND_NEW -> CollectionUtil.replace(logEntity.values1, newValues1)
                        ValuesRelationship.SINGLE_VALUE_COLUMN -> {
                            for (v in newValues0) {
                                if (!logEntity.values0.contains(v)) {
                                    logEntity.values0 += v
                                }
                            }
                        }
                        ValuesRelationship.ADD_AND_REMOVE -> {
                            for (v in newValues0) {
                                if (!logEntity.values0.contains(v)) {
                                    logEntity.values0 += v
                                }
                            }
                            for (v in newValues1) {
                                if (!logEntity.values1.contains(v)) {
                                    logEntity.values1 += v
                                }
                            }
                        }
                    }
                    return
                }
            }

            val log = BotLogEntity(UUID.randomUUID(), guildId, event, memberId, newTargetMemberIds,
                    Instant.now().epochSecond, null, newValues0.toMutableList(), newValues1)
            entityManager.persist(log)

            lastLogCache.put(guildId, log)
        }

        @JvmStatic
        fun oldNewToAddRemove(old: List<Any>, new: List<Any>): Pair<List<String>, List<String>> {
            val oldStrings = old.map { it.toString() }
                    .toList()
            val newStrings = new.map { it.toString() }
                    .toList()

            val addList = newStrings.filter { !oldStrings.contains(it) }
                    .toList()
            val removeList = oldStrings.filter { !newStrings.contains(it) }
                    .toList()
            return Pair(addList, removeList)
        }

        @JvmStatic
        fun findAll(entityManager: EntityManagerWrapper, guildId: Long): List<BotLogEntity> {
            return entityManager.findAllWithValue(BotLogEntity::class.java, "guildId", guildId)
                    .sortedByDescending { it.timestampCreate }
        }

        @JvmStatic
        fun cleanUp(entityManager: EntityManagerWrapper): Int {
            val query = "db.BotLog.deleteMany( { \"timestampCreate\" : { \$lte: :time } } )"
                    .replace(":time", Instant.now().minus(Duration.ofDays(7)).epochSecond.toString())
            return entityManager.createNativeQuery(query, BotLogEntity::class.java)
                    .executeUpdate()
        }

        private fun anyValueToMutableStringList(value: Any?): MutableList<String> {
            if (value == null) {
                return mutableListOf()
            }
            if (value is List<*>) {
                return value
                        .map { it.toString() }
                        .toMutableList()
            }
            return mutableListOf(value.toString())
        }
    }

}