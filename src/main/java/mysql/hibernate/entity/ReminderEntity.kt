package mysql.hibernate.entity

import constants.Language
import core.ShardManager
import core.cache.ServerPatreonBoostCache
import mysql.hibernate.InstantConverter
import mysql.hibernate.template.HibernateEntity
import net.dv8tion.jda.api.entities.Message
import java.time.Instant
import java.util.*
import javax.persistence.*


@Entity(name = "Reminder")
class ReminderEntity(@Id val id: UUID? = null) : HibernateEntity() {

    enum class Type { GUILD_REMINDER, DM_REMINDER }


    var targetId: Long = 0L

    var guildChannelId: Long? = null

    @Convert(converter = InstantConverter::class)
    var triggerTime: Instant = Instant.MIN

    var message: String = ""

    var intervalMinutes: Int? = null
    val intervalMinutesEffectively: Int?
        get() = if (type == Type.GUILD_REMINDER && ServerPatreonBoostCache.get(targetId)) {
            intervalMinutes
        } else {
            null
        }

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    private var _language: Language? = null
    var language: Language
        get() = _language ?: Language.EN
        set(value) {
            _language = value
        }

    var confirmationMessageGuildId: Long? = null

    var confirmationMessageChannelId: Long? = null

    var confirmationMessageMessageId: Long? = null

    val type: Type
        get() = if (guildChannelId != null) {
            Type.GUILD_REMINDER
        } else {
            Type.DM_REMINDER
        }

    val valid: Boolean
        get() = type == Type.DM_REMINDER ||
                (ShardManager.guildIsManaged(targetId) && ShardManager.getLocalGuildById(targetId).isPresent)


    constructor() : this(null)

    companion object {
        @JvmStatic
        @JvmOverloads
        fun createGuildReminder(guildId: Long, channelId: Long, triggerTime: Instant, message: String, confirmationMessage: Message?, intervalMinutes: Int? = null): ReminderEntity {
            return createGuildReminder(guildId, channelId, triggerTime, message, confirmationMessage?.guildIdLong,
                    confirmationMessage?.channelIdLong, confirmationMessage?.idLong, intervalMinutes)
        }

        @JvmStatic
        @JvmOverloads
        fun createGuildReminder(guildId: Long,
                                channelId: Long,
                                triggerTime: Instant,
                                message: String,
                                confirmationMessageGuildId: Long?,
                                confirmationMessageChannelId: Long?,
                                confirmationMessageMessageId: Long?,
                                intervalMinutes: Int? = null
        ): ReminderEntity {
            val reminderEntity = ReminderEntity(UUID.randomUUID())
            reminderEntity.targetId = guildId
            reminderEntity.guildChannelId = channelId
            reminderEntity.triggerTime = triggerTime
            reminderEntity.message = message
            if (intervalMinutes != null && intervalMinutes > 0) {
                reminderEntity.intervalMinutes = intervalMinutes
            }
            reminderEntity.confirmationMessageGuildId = confirmationMessageGuildId
            reminderEntity.confirmationMessageChannelId = confirmationMessageChannelId
            reminderEntity.confirmationMessageMessageId = confirmationMessageMessageId
            return reminderEntity
        }

        @JvmStatic
        fun createDmReminder(userId: Long, triggerTime: Instant, message: String, language: Language, confirmationMessage: Message?): ReminderEntity {
            val reminderEntity = ReminderEntity(UUID.randomUUID())
            reminderEntity.targetId = userId
            reminderEntity.triggerTime = triggerTime
            reminderEntity.message = message
            reminderEntity.language = language
            reminderEntity.confirmationMessageGuildId = confirmationMessage?.guildIdLong
            reminderEntity.confirmationMessageChannelId = confirmationMessage?.channelIdLong
            reminderEntity.confirmationMessageMessageId = confirmationMessage?.idLong
            return reminderEntity
        }
    }

}