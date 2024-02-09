package mysql.hibernate.entity

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import core.ShardManager
import core.atomicassets.AtomicMember
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
        INTEGER, DOUBLE, BOOLEAN, STRING,
        TEXT_KEY, COMMAND_CATEGORY, DURATION,
        CHANNEL, ROLE, MEMBER
    }

    enum class ValuesRelationship {
        EMPTY, OLD_AND_NEW, ADD_AND_REMOVE, SINGLE_VALUE_COLUMN
    }

    enum class Event(val valuesRelationship: ValuesRelationship = ValuesRelationship.EMPTY, val valueType: ValueType? = null) {
        NULL,
        LANGUAGE(ValuesRelationship.OLD_AND_NEW, ValueType.TEXT_KEY),
        PREFIX(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        AUTO_QUOTE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        REMOVE_AUTHOR_MESSAGE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        COMMAND_MANAGEMENT(ValuesRelationship.ADD_AND_REMOVE, ValueType.COMMAND_CATEGORY),
        CHANNEL_WHITELIST(ValuesRelationship.ADD_AND_REMOVE, ValueType.CHANNEL),
        COMMAND_PERMISSIONS_TRANSFER,
        FISHERY_STATUS(ValuesRelationship.OLD_AND_NEW, ValueType.TEXT_KEY),
        FISHERY_DATA_RESET,
        FISHERY_TREASURE_CHESTS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        FISHERY_POWER_UPS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        FISHERY_FISH_REMINDERS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        FISHERY_COIN_GIFT_LIMIT(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        FISHERY_TREASURE_CHEST_PROBABILITY(ValuesRelationship.OLD_AND_NEW, ValueType.DOUBLE),
        FISHERY_POWER_UP_PROBABILITY(ValuesRelationship.OLD_AND_NEW, ValueType.DOUBLE),
        FISHERY_EXCLUDED_CHANNELS(ValuesRelationship.ADD_AND_REMOVE, ValueType.CHANNEL),
        FISHERY_VOICE_HOURS_LIMIT(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        FISHERY_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE),
        FISHERY_ROLES_UPGRADE_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL),
        FISHERY_ROLES_PRICE_MIN(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        FISHERY_ROLES_PRICE_MAX(ValuesRelationship.OLD_AND_NEW, ValueType.INTEGER),
        FISHERY_ROLES_SINGLE_ROLES(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        FISHERY_MANAGE_FISH(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_COINS(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_DAILY_STREAK(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_ROD(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_ROBOT(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_NET(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_METAL_DETECTOR(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_ROLE(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_SURVEYS(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_WORK(ValuesRelationship.OLD_AND_NEW, ValueType.STRING),
        FISHERY_MANAGE_RESET,
        ALERTS(ValuesRelationship.ADD_AND_REMOVE, ValueType.STRING),
        NSFW_FILTER(ValuesRelationship.ADD_AND_REMOVE, ValueType.STRING),
        MOD_NOTIFICATION_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL),
        MOD_CONFIRMATION_MESSAGES(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        MOD_JAIL_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE),
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
        INVITE_FILTER_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        INVITE_FILTER_EXCLUDED_MEMBERS(ValuesRelationship.ADD_AND_REMOVE, ValueType.MEMBER),
        INVITE_FILTER_EXCLUDED_CHANNELS(ValuesRelationship.ADD_AND_REMOVE, ValueType.CHANNEL),
        INVITE_FILTER_LOG_RECEIVERS(ValuesRelationship.ADD_AND_REMOVE, ValueType.MEMBER),
        INVITE_FILTER_ACTION(ValuesRelationship.OLD_AND_NEW, ValueType.TEXT_KEY),
        WORD_FILTER_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        WORD_FILTER_EXCLUDED_MEMBERS(ValuesRelationship.ADD_AND_REMOVE, ValueType.MEMBER),
        WORD_FILTER_LOG_RECEIVERS(ValuesRelationship.ADD_AND_REMOVE, ValueType.MEMBER),
        WORD_FILTER_WORDS(ValuesRelationship.ADD_AND_REMOVE, ValueType.STRING),
        INVITE_TRACKING_ACTIVE(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        INVITE_TRACKING_LOG_CHANNEL(ValuesRelationship.OLD_AND_NEW, ValueType.CHANNEL),
        INVITE_TRACKING_PING_MEMBERS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        INVITE_TRACKING_ADVANCED_STATISTICS(ValuesRelationship.OLD_AND_NEW, ValueType.BOOLEAN),
        INVITE_TRACKING_FAKE_INVITES(ValuesRelationship.ADD_AND_REMOVE, ValueType.MEMBER),
        INVITE_TRACKING_FAKE_INVITES_RESET,
        INVITE_TRACKING_RESET,
        GIVEAWAYS_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        GIVEAWAYS_EDIT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        GIVEAWAYS_END(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        GIVEAWAYS_REMOVE(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        GIVEAWAYS_REROLL(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        REACTION_ROLES_ADD(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        REACTION_ROLES_EDIT(ValuesRelationship.SINGLE_VALUE_COLUMN, ValueType.STRING),
        AUTO_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE),
        AUTO_ROLES_SYNC,
        STICKY_ROLES(ValuesRelationship.ADD_AND_REMOVE, ValueType.ROLE),
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
                    entityManager.transaction.begin()
                    logEntity.timestampUpdate = Instant.now().epochSecond

                    when (event.valuesRelationship) {
                        ValuesRelationship.EMPTY -> {}
                        ValuesRelationship.OLD_AND_NEW -> logEntity.values1 = newValues1
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

                    entityManager.transaction.commit()
                    return
                }
            }

            val log = BotLogEntity(UUID.randomUUID(), guildId, event, memberId, newTargetMemberIds,
                    Instant.now().epochSecond, null, newValues0.toMutableList(), newValues1)
            entityManager.transaction.begin()
            entityManager.persist(log)
            entityManager.transaction.commit()

            entityManager.detach(log)
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