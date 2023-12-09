package mysql.hibernate.entity.guild

import constants.Language
import core.assets.GuildAsset
import core.cache.ServerPatreonBoostCache
import mysql.hibernate.entity.ReminderEntity
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.SortNatural
import java.util.*
import javax.persistence.*


@Entity(name = "Guild")
class GuildEntity(key: String) : HibernateEntity(), GuildAsset {

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
    var language: Language
        get() = _language ?: Language.EN
        set(value) {
            _language = value
        }
    val locale: Locale
        get() = language.locale

    @Column(name = "removeAuthorMessage")
    private var _removeAuthorMessage: Boolean? = null
    var removeAuthorMessage: Boolean
        get() = _removeAuthorMessage ?: false
        set(value) {
            _removeAuthorMessage = value
        }
    val removeAuthorMessageEffectively: Boolean
        get() = removeAuthorMessage && ServerPatreonBoostCache.get(guildId.toLong())

    @Embedded
    @Column(name = FISHERY)
    val fishery = FisheryEntity()

    @Embedded
    @Column(name = INVITE_FILTER)
    val inviteFilter = InviteFilterEntity()

    @Embedded
    @Column(name = WORD_FILTER)
    val wordFilter = WordFilterEntity()

    @Embedded
    @Column(name = STICKY_ROLES)
    val stickyRoles = StickyRolesEntity()

    @ElementCollection
    @SortNatural
    val customCommands = sortedMapOf<String, CustomCommandEntity>()

    @ElementCollection
    val commandChannelShortcuts = mutableMapOf<Long, String>()

    val reminders: List<ReminderEntity>
        get() = entityManager.findAllWithValue(ReminderEntity::class.java, "targetId", guildId.toLong())


    constructor() : this("0")

    override fun getGuildId(): Long {
        return guildId.toLong()
    }

    @PostLoad
    override fun postLoad() {
        fishery.postLoad(this)
        inviteFilter.postLoad(this)
        wordFilter.postLoad(this)
        stickyRoles.postLoad(this)
        customCommands.values.forEach { it.postLoad(this) }
    }

    override fun postRemove() {
        entityManager.deleteAllWithValue(ReminderEntity::class.java, "targetId", guildId.toLong())
    }

}
