package mysql.hibernate.entity.guild

import constants.Language
import core.assets.GuildAsset
import core.cache.ServerPatreonBoostCache
import mysql.hibernate.entity.ReminderEntity
import mysql.hibernate.entity.assets.LanguageAsset
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.SortNatural
import java.time.LocalDate
import javax.persistence.*


@Entity(name = "Guild")
class GuildEntity(key: String) : HibernateEntity(), GuildAsset, LanguageAsset {

    @Id
    private val guildId = key

    @Column(name = "prefix")
    private var _prefix: String? = null
    var prefix: String
        get() = _prefix ?: "L."
        set(value) {
            _prefix = value
        }

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    private var _language: Language? = null
    override var language: Language
        get() = _language ?: Language.EN
        set(value) {
            _language = value
        }

    @Column(name = "removeAuthorMessage")
    private var _removeAuthorMessage: Boolean? = null
    var removeAuthorMessage: Boolean
        get() = _removeAuthorMessage ?: false
        set(value) {
            _removeAuthorMessage = value
        }
    val removeAuthorMessageEffectively: Boolean
        get() = removeAuthorMessage && ServerPatreonBoostCache.get(guildId.toLong())

    @Column(name = "txt2imgBanned")
    private var _txt2imgBanned: Boolean? = null
    var txt2imgBanned: Boolean
        get() = _txt2imgBanned ?: false
        set(value) {
            _txt2imgBanned = value
        }

    var latestPresentDate: LocalDate? = LocalDate.now()

    @Embedded
    @Column(name = FISHERY)
    val fishery = FisheryEntity()

    @Embedded
    @Column(name = MODERATION)
    val moderation = ModerationEntity()

    @Embedded
    @Column(name = INVITE_FILTER)
    val inviteFilter = InviteFilterEntity()

    @Embedded
    @Column(name = WORD_FILTER)
    val wordFilter = WordFilterEntity()

    @Embedded
    @Column(name = STICKY_ROLES)
    val stickyRoles = StickyRolesEntity()

    @Embedded
    @Column(name = TICKETS)
    val tickets = TicketsEntity()

    @ElementCollection
    @SortNatural
    val customCommands = sortedMapOf<String, CustomCommandEntity>()

    @ElementCollection
    val commandChannelShortcuts = mutableMapOf<Long, String>()

    @ElementCollection
    val disabledCommandsAndCategories = mutableSetOf<String>()

    val reminders: List<ReminderEntity>
        get() = entityManager.findAllWithValue(ReminderEntity::class.java, "targetId", guildId.toLong())


    constructor() : this("0")

    override fun getGuildId(): Long {
        return guildId.toLong()
    }

    @PostLoad
    override fun postLoad() {
        fishery.postLoad(this)
        moderation.postLoad(this)
        inviteFilter.postLoad(this)
        wordFilter.postLoad(this)
        stickyRoles.postLoad(this)
        tickets.postLoad(this)
    }

    override fun postRemove() {
        entityManager.deleteAllWithValue(ReminderEntity::class.java, "targetId", guildId.toLong())
    }

}
